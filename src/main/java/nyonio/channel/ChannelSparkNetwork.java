package nyonio.channel;

import appeng.api.AEApi;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.GridFlags;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridConnection;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.tile.networking.TileController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
import nyonio.ChannelSparkConfig;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.common.network.PacketBotaniaEffect;
import vazkii.botania.common.network.PacketHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Maintains the wireless AE2 links represented by channel sparks.
 *
 * AE2's public grid, part, and connection APIs are used directly. This keeps
 * the channel bridge type-safe and avoids reflective calls on every spark
 * search/tick.
 */
public final class ChannelSparkNetwork {
    private static final Map<IGridConnection, Integer> CHANNEL_CAPACITIES =
            Collections.synchronizedMap(new WeakHashMap<IGridConnection, Integer>());
    private static final Set<IGridConnection> WIRELESS_CONNECTIONS =
            Collections.synchronizedSet(Collections.newSetFromMap(
                    new WeakHashMap<IGridConnection, Boolean>()));
    private static final Map<BridgeKey, BridgeRecord> AE_BRIDGES = new HashMap<>();
    private static final Map<UUID, BridgeProxy> MAIN_PROXIES = new HashMap<>();
    private static final ThreadLocal<Integer> PENDING_CHANNEL_CAPACITY = new ThreadLocal<>();

    /** Deterministic AE2 controller-face order: +X,+Y,+Z,-X,-Y,-Z. */
    private static final EnumFacing[] CHANNEL_DIRECTION_ORDER = {
            EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH,
            EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.NORTH
    };

    private ChannelSparkNetwork() {
    }

    public static Integer getCapacity(IGridConnection connection) {
        Integer capacity = connection == null ? null : CHANNEL_CAPACITIES.get(connection);
        return capacity == null ? PENDING_CHANNEL_CAPACITY.get() : capacity;
    }

    public static boolean isWirelessConnection(IGridConnection connection) {
        return connection != null && WIRELESS_CONNECTIONS.contains(connection);
    }

    private static void registerConnection(Object connection) {
        if (connection instanceof IGridConnection) {
            IGridConnection gridConnection = (IGridConnection) connection;
            CHANNEL_CAPACITIES.put(gridConnection, ChannelSparkConfig.getChannelCapacity());
            WIRELESS_CONNECTIONS.add(gridConnection);
        }
    }

    public static final class Link {
        private final EntityChannelSpark other;
        private final Object connection;

        private Link(EntityChannelSpark other, Object connection) {
            this.other = other;
            this.connection = connection;
        }
    }

    private static final class BridgeKey {
        private final UUID first;
        private final UUID second;

        private BridgeKey(UUID first, UUID second) {
            if (first.compareTo(second) <= 0) {
                this.first = first;
                this.second = second;
            } else {
                this.first = second;
                this.second = first;
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof BridgeKey)) {
                return false;
            }
            BridgeKey other = (BridgeKey) object;
            return first.equals(other.first) && second.equals(other.second);
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }
    }

    /** Lightweight IGridProxyable host used by an AE2-style outer proxy. */
    /** Lightweight IGridProxyable host used by an AE2-style outer proxy. */
    private static final class ProxyHost implements IGridProxyable {
        private final IGridNode anchor;
        private final DimensionalCoord location;
        private AENetworkProxy proxy;

        private ProxyHost(IGridNode anchor, DimensionalCoord location) {
            this.anchor = anchor;
            this.location = location;
        }

        private void setProxy(AENetworkProxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public AENetworkProxy getProxy() {
            return proxy;
        }

        @Override
        public DimensionalCoord getLocation() {
            return location;
        }

        @Override
        public void gridChanged() {
            // The proxy is ephemeral; its parent spark rebuilds it as needed.
        }

        @Override
        public IGridNode getGridNode(AEPartLocation direction) {
            return anchor;
        }

        @Override
        public void securityBreak() {
            // There is no physical block to break for a channel spark proxy.
        }

        @Override
        public AECableType getCableConnectionType(AEPartLocation direction) {
            return AECableType.NONE;
        }
    }

    /**
     * Mirrors PartP2PTunnelME.outerProxy. The proxy node is attached to one
     * real AE endpoint, while the two proxy nodes form the logical P2P link.
     */
    /**
     * Mirrors PartP2PTunnelME.outerProxy. This node is a separate AE2
     * external-facing node; it is never the actual cable/controller endpoint.
     */
    /**
     * One AE2 outer proxy for one spark endpoint. The main channel spark owns
     * one shared proxy, matching the outerProxy used by an ME P2P input.
     */
    private static final class BridgeProxy {
        private final AENetworkProxy proxy;
        private final IGridNode anchor;
        private final AEPartLocation direction;
        private final List<IGridConnection> fallbackAttachments = new ArrayList<>();

        private BridgeProxy(BridgeEndpoint endpoint, World world,
                            BlockPos targetPos, EntityChannelSpark spark) {
            this.anchor = endpoint == null ? null : endpoint.node;
            this.direction = endpoint == null ? null : endpoint.direction;
            EnumFacing face = getProxyAttachmentFace(endpoint, spark);
            BlockPos proxyPos = face == null ? targetPos : targetPos.offset(face);
            DimensionalCoord location = new DimensionalCoord(world, proxyPos);
            ProxyHost host = new ProxyHost(this.anchor, location);
            this.proxy = new AENetworkProxy(
                    host, "botania_applie_channel_spark", ItemStack.EMPTY, true);
            host.setProxy(this.proxy);
            this.proxy.setFlags(GridFlags.DENSE_CAPACITY, GridFlags.CANNOT_CARRY_COMPRESSED);
            if (face != null) {
                this.proxy.setValidSides(EnumSet.of(face.getOpposite()));
            }
            this.proxy.onReady();
        }

        private IGridNode node() {
            return proxy.getNode();
        }

        private boolean matches(BridgeEndpoint endpoint) {
            return endpoint != null && anchor == endpoint.node
                    && direction == endpoint.direction
                    && node() != null && safeGrid(node()) != null
                    && safeGrid(node()) == safeGrid(endpoint.node);
        }

        private void destroy() {
            destroyConnections(fallbackAttachments);
            proxy.invalidate();
        }
    }

    private static EnumFacing getProxyAttachmentFace(BridgeEndpoint endpoint,
                                                       EntityChannelSpark spark) {
        if (endpoint != null && endpoint.direction != null
                && endpoint.direction != AEPartLocation.INTERNAL
                && endpoint.direction.getFacing() != null) {
            return endpoint.direction.getFacing();
        }
        AEPartLocation direction = getCableAttachmentDirection(spark);
        return direction == null || direction == AEPartLocation.INTERNAL
                ? EnumFacing.UP : direction.getFacing();
    }


    private static final class BridgeBuild {
        private final List<IGridConnection> connections = new ArrayList<>();

        private final List<BridgeProxy> proxies = new ArrayList<>();

        private boolean isEmpty() {
            return connections.isEmpty();
        }
    }

    private static final class BridgeEndpoint {
        private final IGridNode node;
        private final AEPartLocation direction;
        private final boolean controllerFace;

        private BridgeEndpoint(IGridNode node, AEPartLocation direction) {
            this(node, direction, false);
        }

        private BridgeEndpoint(IGridNode node, AEPartLocation direction,
                               boolean controllerFace) {
            this.node = node;
            this.direction = direction;
            this.controllerFace = controllerFace;
        }
    }

    private static final class BridgeRecord {
        private final EntityChannelSpark first;
        private final EntityChannelSpark second;
        private final List<IGridConnection> connections;
        private final List<BridgeProxy> proxies;
        private final int expectedConnections;

        private BridgeRecord(EntityChannelSpark first, EntityChannelSpark second,
                             BridgeBuild build, int expectedConnections) {
            this.first = first;
            this.second = second;
            this.connections = new ArrayList<>(build.connections);
            this.proxies = new ArrayList<>(build.proxies);
            this.expectedConnections = expectedConnections;
        }
    }

    public static boolean hasSparkInBlock(World world, BlockPos blockPos) {
        if (world == null || blockPos == null) {
            return false;
        }
        AxisAlignedBB bounds = new AxisAlignedBB(blockPos, blockPos.add(1, 1, 1));
        for (EntityChannelSpark spark : world.getEntitiesWithinAABB(EntityChannelSpark.class, bounds)) {
            if (!spark.isDead && blockPos.equals(spark.getContainingBlock())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPrimaryInBlock(EntityChannelSpark spark) {
        if (spark == null || spark.isDead || spark.world == null) {
            return false;
        }
        BlockPos blockPos = spark.getContainingBlock();
        AxisAlignedBB bounds = new AxisAlignedBB(blockPos, blockPos.add(1, 1, 1));
        EntityChannelSpark first = null;
        for (EntityChannelSpark candidate : spark.world.getEntitiesWithinAABB(EntityChannelSpark.class, bounds)) {
            if (candidate.isDead || !blockPos.equals(candidate.getContainingBlock())) {
                continue;
            }
            if (first == null || isEarlier(candidate, first)) {
                first = candidate;
            }
        }
        return first == spark;
    }

    private static boolean isEarlier(EntityChannelSpark first, EntityChannelSpark second) {
        if (first.getPlacementOrder() != second.getPlacementOrder()) {
            return first.getPlacementOrder() < second.getPlacementOrder();
        }
        return first.getEntityId() < second.getEntityId();
    }

    private static boolean hasUsableEndpoint(EntityChannelSpark spark) {
        return spark != null && spark.hasTarget()
                && !findBridgeEndpoints(spark.world, spark.getTargetPos()).isEmpty();
    }

    public static void showNetwork(EntityPlayer player, EntityChannelSpark source) {
        if (player == null || source == null || source.world == null || source.world.isRemote) {
            return;
        }
        Set<UUID> visited = new HashSet<>();
        List<EntityChannelSpark> queue = new ArrayList<>();
        visited.add(source.getUniqueID());
        queue.add(source);

        for (int index = 0; index < queue.size(); index++) {
            EntityChannelSpark current = queue.get(index);
            for (Link link : new ArrayList<>(current.getLinks().values())) {
                EntityChannelSpark other = link.other;
                if (other == null || other.isDead || !isPrimaryInBlock(other)
                        || !visited.add(other.getUniqueID())) {
                    continue;
                }
                vazkii.botania.common.entity.EntitySpark.particleBeam(player, current, other);
                queue.add(other);
            }
        }
    }

    public static void tick(EntityChannelSpark spark) {
        if (spark == null || spark.isDead || spark.world == null || spark.world.isRemote) {
            return;
        }

        if (!isPrimaryInBlock(spark)) {
            clear(spark);
            return;
        }

        removeInvalidLinks(spark);
        int radius = ChannelSparkConfig.getTransferRadius();
        AxisAlignedBB search = new AxisAlignedBB(
                spark.posX - radius, spark.posY - radius, spark.posZ - radius,
                spark.posX + radius, spark.posY + radius, spark.posZ + radius);
        List<EntityChannelSpark> nearby = spark.world.getEntitiesWithinAABB(EntityChannelSpark.class, search);
        for (EntityChannelSpark other : nearby) {
            if (other == spark || other.isDead || other.world != spark.world
                    || !isPrimaryInBlock(other)) {
                continue;
            }
            if (spark.getDistanceSq(other) > (double) radius * (double) radius) {
                continue;
            }
            connect(spark, other);
        }

        cleanupBridgeRecords();
        reconcileAeBridges(spark);
    }

    public static void clear(EntityChannelSpark spark) {
        if (spark == null) {
            return;
        }
        List<Link> links = new ArrayList<>(spark.getLinks().values());
        for (Link link : links) {
            unlink(spark, link.other, link.connection);
        }
        spark.getLinks().clear();
        removeBridgesForSpark(spark);
    }

    private static void removeInvalidLinks(EntityChannelSpark spark) {
        int radius = ChannelSparkConfig.getTransferRadius();
        double maxDistance = (double) radius * (double) radius;
        List<Link> links = new ArrayList<>(spark.getLinks().values());
        for (Link link : links) {
            EntityChannelSpark other = link.other;
            if (other == null || other.isDead || other.world != spark.world
                    || spark.getDistanceSq(other) > maxDistance
                    || !isPrimaryInBlock(other)) {
                unlink(spark, other, link.connection);
            } else if (link.connection != null
                    && (!hasUsableEndpoint(spark) || !hasUsableEndpoint(other))) {
                downgradeConnection(spark, other, link.connection);
            }
        }
    }

    private static void connect(EntityChannelSpark first, EntityChannelSpark second) {
        Link existing = first.getLinks().get(second.getUniqueID());
        if (existing != null) {
            return;
        }

        // Keep the logical spark graph independent from AE endpoints. The
        // whole component is reconciled below, which also supports a chain
        // containing floating sparks between two AE networks.
        first.getLinks().put(second.getUniqueID(), new Link(second, null));
        second.getLinks().put(first.getUniqueID(), new Link(first, null));
    }

    private static void reconcileAeBridges(EntityChannelSpark source) {
        List<EntityChannelSpark> network = collectLogicalNetwork(source);
        if (network.isEmpty()) {
            return;
        }

        EntityChannelSpark main = null;
        for (EntityChannelSpark candidate : network) {
            if (candidate != null && candidate.isMainChannelSpark()
                    && hasControllerEndpoint(candidate)
                    && (main == null || isEarlier(candidate, main))) {
                main = candidate;
            }
        }
        if (main == null) {
            removeStaleNonSourceBridges(network, null);
            for (EntityChannelSpark candidate : network) {
                if (candidate != null && candidate.isMainChannelSpark()) {
                    destroyMainProxy(candidate);
                }
            }
            return;
        }

        // One shared main outer proxy is the P2P hub. Each ordinary spark
        // gets one independent remote outer proxy and one bridge record.
        BridgeProxy mainProxy = getOrCreateMainProxy(main);
        if (mainProxy == null) {
            removeStaleNonSourceBridges(network, null);
            destroyMainProxy(main);
            return;
        }

        List<EntityChannelSpark> endpointSparks = new ArrayList<>();
        for (EntityChannelSpark spark : network) {
            if (spark != null && spark != main && !spark.isMainChannelSpark()
                    && hasUsableEndpoint(spark)) {
                endpointSparks.add(spark);
            }
        }

        removeStaleNonSourceBridges(network, main);
        removeUndesiredBridges(main, endpointSparks);
        for (EntityChannelSpark other : endpointSparks) {
            BridgeKey key = new BridgeKey(main.getUniqueID(), other.getUniqueID());
            int expectedConnections = expectedBridgeConnectionCount(main, other);
            BridgeRecord existing = AE_BRIDGES.get(key);
            if (existing != null) {
                if (existing.expectedConnections >= expectedConnections
                        && areConnectionsAlive(existing.connections)) {
                    continue;
                }
                destroyBridge(existing);
                AE_BRIDGES.remove(key);
            }
            BridgeBuild build = createGridConnectionsIfPossible(main, other, mainProxy);
            if (!build.isEmpty()) {
                AE_BRIDGES.put(key, new BridgeRecord(main, other, build,
                        expectedConnections));
            }
        }
    }

    private static BridgeProxy getOrCreateMainProxy(EntityChannelSpark main) {
        if (main == null || main.world == null || main.getTargetPos() == null) {
            return null;
        }
        List<BridgeEndpoint> endpoints =
                findBridgeEndpoints(main.world, main.getTargetPos());
        BridgeEndpoint endpoint = selectP2PEndpoint(endpoints);
        if (endpoint == null) {
            return null;
        }

        UUID id = main.getUniqueID();
        BridgeProxy existing = MAIN_PROXIES.get(id);
        if (existing != null && existing.matches(endpoint)) {
            return existing;
        }
        if (existing != null) {
            removeBridgeRecordsForMain(main);
            existing.destroy();
            MAIN_PROXIES.remove(id);
        }

        BridgeProxy created = new BridgeProxy(endpoint, main.world,
                main.getTargetPos(), main);
        if (!attachBridgeProxy(created, endpoint)) {
            created.destroy();
            return null;
        }
        MAIN_PROXIES.put(id, created);
        return created;
    }

    private static void destroyMainProxy(EntityChannelSpark main) {
        if (main == null) {
            return;
        }
        BridgeProxy proxy = MAIN_PROXIES.remove(main.getUniqueID());
        if (proxy != null) {
            proxy.destroy();
        }
    }

    private static void removeBridgeRecordsForMain(EntityChannelSpark main) {
        if (main == null) {
            return;
        }
        java.util.Iterator<Map.Entry<BridgeKey, BridgeRecord>> iterator =
                AE_BRIDGES.entrySet().iterator();
        while (iterator.hasNext()) {
            BridgeRecord record = iterator.next().getValue();
            if (record.first == main) {
                destroyBridge(record);
                iterator.remove();
            }
        }
    }

    private static void removeUndesiredBridges(EntityChannelSpark main,
                                               List<EntityChannelSpark> endpoints) {
        Set<UUID> desired = new HashSet<>();
        if (endpoints != null) {
            for (EntityChannelSpark endpoint : endpoints) {
                if (endpoint != null) {
                    desired.add(endpoint.getUniqueID());
                }
            }
        }
        java.util.Iterator<Map.Entry<BridgeKey, BridgeRecord>> iterator =
                AE_BRIDGES.entrySet().iterator();
        while (iterator.hasNext()) {
            BridgeRecord record = iterator.next().getValue();
            if (record.first == main && (record.second == null
                    || !desired.contains(record.second.getUniqueID()))) {
                destroyBridge(record);
                iterator.remove();
            }
        }
    }

    /**
     * Selects the logical main channel spark. A controller-attached spark is
     * always preferred, and ties are resolved by placement order. When no
     * controller is attached, placement order still makes the fallback stable
     * instead of depending on the world's entity-list order.
     */
    private static EntityChannelSpark selectChannelSource(
            List<EntityChannelSpark> endpointSparks) {
        if (endpointSparks == null || endpointSparks.isEmpty()) {
            return null;
        }
        Collections.sort(endpointSparks, new Comparator<EntityChannelSpark>() {
            @Override
            public int compare(EntityChannelSpark first, EntityChannelSpark second) {
                boolean firstController = hasControllerEndpoint(first);
                boolean secondController = hasControllerEndpoint(second);
                if (firstController != secondController) {
                    return firstController ? -1 : 1;
                }
                if (first.getPlacementOrder() != second.getPlacementOrder()) {
                    return first.getPlacementOrder() < second.getPlacementOrder() ? -1 : 1;
                }
                return Integer.compare(first.getEntityId(), second.getEntityId());
            }
        });
        return endpointSparks.get(0);
    }

    private static boolean hasControllerEndpoint(EntityChannelSpark spark) {
return spark != null && !spark.isDead && spark.world != null
                && spark.getTargetPos() != null
                && spark.world.getTileEntity(spark.getTargetPos()) instanceof TileController;
    }

    /**
     * If a controller becomes the source after the logical network was
     * already bridged, remove old non-source-to-non-source connections before
     * creating the new star topology.
     */
    private static void removeStaleNonSourceBridges(
            List<EntityChannelSpark> network, EntityChannelSpark source) {
if (network == null || network.isEmpty()) {
            return;
        }
        Set<UUID> members = new HashSet<>();
        for (EntityChannelSpark spark : network) {
            if (spark != null) members.add(spark.getUniqueID());
        }
        java.util.Iterator<Map.Entry<BridgeKey, BridgeRecord>> iterator = AE_BRIDGES.entrySet().iterator();
        while (iterator.hasNext()) {
            BridgeRecord record = iterator.next().getValue();
            if (record.first == null || record.second == null
                    || !members.contains(record.first.getUniqueID())
                    || !members.contains(record.second.getUniqueID())) continue;
            if (source == null || (record.first != source && record.second != source)) {
                destroyBridge(record);
                iterator.remove();
            }
        }
    }

    private static List<EntityChannelSpark> collectLogicalNetwork(EntityChannelSpark source) {
        List<EntityChannelSpark> network = new ArrayList<>();
        if (source == null || source.isDead || source.world == null) {
            return network;
        }

        Set<UUID> visited = new HashSet<>();
        List<EntityChannelSpark> queue = new ArrayList<>();
        queue.add(source);
        visited.add(source.getUniqueID());
        for (int index = 0; index < queue.size(); index++) {
            EntityChannelSpark current = queue.get(index);
            if (current == null || current.isDead || current.world != source.world
                    || !isPrimaryInBlock(current)) {
                continue;
            }
            network.add(current);
            for (Link link : new ArrayList<>(current.getLinks().values())) {
                EntityChannelSpark other = link.other;
                if (other != null && !other.isDead && other.world == source.world
                        && isPrimaryInBlock(other) && visited.add(other.getUniqueID())) {
                    queue.add(other);
                }
            }
        }
        return network;
    }

    private static void cleanupBridgeRecords() {
        java.util.Iterator<Map.Entry<BridgeKey, BridgeRecord>> iterator =
                AE_BRIDGES.entrySet().iterator();
        while (iterator.hasNext()) {
            BridgeRecord record = iterator.next().getValue();
            boolean valid = record.first != null && record.second != null
                    && !record.first.isDead && !record.second.isDead
                    && record.first.world == record.second.world
                    && isPrimaryInBlock(record.first) && isPrimaryInBlock(record.second)
                    && areLogicallyConnected(record.first, record.second)
                    && areConnectionsAlive(record.connections);
            if (!valid) {
                destroyBridge(record);
                iterator.remove();
            }
        }
    }

    private static boolean areLogicallyConnected(EntityChannelSpark first,
                                                  EntityChannelSpark second) {
        for (EntityChannelSpark candidate : collectLogicalNetwork(first)) {
            if (candidate == second) {
                return true;
            }
        }
        return false;
    }

    private static boolean areConnectionsAlive(List<IGridConnection> connections) {
        if (connections == null || connections.isEmpty()) {
            return false;
        }
        for (IGridConnection connection : connections) {
            if (!isConnectionAlive(connection)) {
                return false;
            }
        }
        return true;
    }

    private static void destroyBridge(BridgeRecord record) {
        if (record == null) {
            return;
        }
        destroyConnections(record.connections);
        for (BridgeProxy proxy : record.proxies) {
            if (proxy != null) {
                proxy.destroy();
            }
        }
    }

    private static void destroyConnections(List<IGridConnection> connections) {
        if (connections == null) {
            return;
        }
        for (IGridConnection connection : new ArrayList<>(connections)) {
            destroyConnection(connection);
        }
    }

    private static boolean isConnectionAlive(IGridConnection connection) {
        if (connection == null) {
            return false;
        }
        try {
            return hasConnection(connection.a(), connection)
                    && hasConnection(connection.b(), connection);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasConnection(IGridNode node, IGridConnection expected) {
        if (node == null || expected == null) {
            return false;
        }
        try {
            for (IGridConnection connection : node.getConnections()) {
                if (connection == expected) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void removeBridgesForSpark(EntityChannelSpark spark) {
        if (spark == null) {
            return;
        }
        java.util.Iterator<Map.Entry<BridgeKey, BridgeRecord>> iterator =
                AE_BRIDGES.entrySet().iterator();
        while (iterator.hasNext()) {
            BridgeRecord record = iterator.next().getValue();
            if (record.first == spark || record.second == spark) {
                destroyBridge(record);
                iterator.remove();
            }
        }
        destroyMainProxy(spark);
    }

    private static int expectedBridgeConnectionCount(
            EntityChannelSpark first, EntityChannelSpark second) {
        return findBridgeEndpoints(first.world, first.getTargetPos()).isEmpty()
                || findBridgeEndpoints(second.world, second.getTargetPos()).isEmpty()
                ? 0 : 1;
    }

    private static BridgeBuild createGridConnectionsIfPossible(
            EntityChannelSpark first, EntityChannelSpark second,
            BridgeProxy mainProxy) {
        BridgeBuild build = new BridgeBuild();
        if (mainProxy == null || mainProxy.node() == null) {
            return build;
        }
        List<BridgeEndpoint> secondEndpoints =
                findBridgeEndpoints(second.world, second.getTargetPos());
        BridgeEndpoint secondEndpoint = selectP2PEndpoint(secondEndpoints);
        if (secondEndpoint == null) {
            return build;
        }

        Object mainGrid = safeGrid(mainProxy.node());
        Object secondGrid = safeGrid(secondEndpoint.node);
        if (mainGrid == null || secondGrid == null || mainGrid == secondGrid) {
            return build;
        }

        BridgeProxy secondProxy = null;
        try {
            secondProxy = new BridgeProxy(secondEndpoint, second.world,
                    second.getTargetPos(), second);
            if (!attachBridgeProxy(secondProxy, secondEndpoint)) {
                secondProxy.destroy();
                return build;
            }

            // Connect one shared main outer proxy to one independent remote
            // outer proxy, just like an ME P2P input/output pair.
            IGridConnection connection = createP2PGridConnection(
                    mainProxy.node(), secondProxy.node());
            if (connection == null) {
                secondProxy.destroy();
                return build;
            }
            build.proxies.add(secondProxy);
            build.connections.add(connection);
            return build;
        } catch (Throwable error) {
            if (secondProxy != null) {
                secondProxy.destroy();
            }
            logConnectionFailure("AE2 channel spark P2P bridge", mainProxy.node(),
                    secondEndpoint.node, error);
            return build;
        }
    }

    private static boolean attachBridgeProxy(BridgeProxy proxy,
                                               BridgeEndpoint endpoint) {
        if (proxy == null || endpoint == null || endpoint.node == null
                || proxy.node() == null) {
            return false;
        }
        Object endpointGrid = safeGrid(endpoint.node);
        Object proxyGrid = safeGrid(proxy.node());
        if (endpointGrid == null || proxyGrid == null) {
            return false;
        }
        if (endpointGrid == proxyGrid) {
            return true;
        }
        try {
            IGridConnection attachment = GridConnection.create(
                    endpoint.node, proxy.node(), endpoint.direction);
            proxy.fallbackAttachments.add(attachment);
            repathAfterConnection(endpoint.node, proxy.node());
            return safeGrid(proxy.node()) == safeGrid(endpoint.node);
        } catch (Throwable error) {
            logConnectionFailure("AE2 channel spark endpoint attachment",
                    endpoint.node, proxy.node(), error);
            return false;
        }
    }

    private static IGridConnection createP2PGridConnection(IGridNode first,
                                                            IGridNode second) {
        if (first == null || second == null) {
            return null;
        }
        PENDING_CHANNEL_CAPACITY.set(ChannelSparkConfig.getChannelCapacity());
        try {
            IGridConnection connection = AEApi.instance().grid()
                    .createGridConnection(first, second);
            registerConnection(connection);
            repathAfterConnection(first, second);
            return connection;
        } catch (Throwable error) {
            logConnectionFailure("AE2 P2P outer connection", first, second, error);
            return null;
        } finally {
            PENDING_CHANNEL_CAPACITY.remove();
        }
    }

    private static List<BridgeEndpoint[]> pairBridgeEndpoints(
            List<BridgeEndpoint> first, List<BridgeEndpoint> second) {
        List<BridgeEndpoint[]> pairs = new ArrayList<>();
        if (first.isEmpty() || second.isEmpty()) {
            return pairs;
        }

        // A controller exposes six logical faces, but one remote spark must
        // consume exactly one of them. Select the first free face in the
        // deterministic +X,+Y,+Z,-X,-Y,-Z order. This preserves independent
        // P2P-style paths for multiple remote networks instead of joining
        // every face of every network like a dense cable.
        BridgeEndpoint firstEndpoint = selectP2PEndpoint(first);
        BridgeEndpoint secondEndpoint = selectP2PEndpoint(second);
        if (firstEndpoint == null || secondEndpoint == null) {
            return pairs;
        }
        pairs.add(new BridgeEndpoint[]{firstEndpoint, secondEndpoint});
        return pairs;
    }

    private static BridgeEndpoint selectP2PEndpoint(List<BridgeEndpoint> endpoints) {
        BridgeEndpoint fallback = null;
        for (BridgeEndpoint endpoint : endpoints) {
            if (!endpoint.controllerFace) {
                if (fallback == null) {
                    fallback = endpoint;
                }
                continue;
            }
            if (fallback == null) {
                fallback = endpoint;
            }
            if (!isBridgeDirectionInUse(endpoint.node, endpoint.direction)) {
                return endpoint;
            }
        }
        return fallback != null && !fallback.controllerFace
                ? fallback : (fallback != null
                && !isBridgeDirectionInUse(fallback.node, fallback.direction)
                ? fallback : null);
    }

    private static boolean isBridgeDirectionInUse(IGridNode node, AEPartLocation direction) {
        if (node == null || direction == null) {
            return true;
        }
        try {
            for (IGridConnection connection : node.getConnections()) {
                if (connection != null && direction == connection.getDirection(node)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            return true;
        }
        return false;
    }

    private static Object safeGrid(IGridNode node) {
        try {
            return node == null ? null : node.getGrid();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasDirectConnection(IGridNode first, IGridNode second) {
        if (first == null || second == null) {
            return false;
        }
        try {
            for (IGridConnection connection : first.getConnections()) {
                if (connection != null && connection.getOtherSide(first) == second) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static AEPartLocation determineBridgeDirection(EntityChannelSpark first,
                                                            IGridNode firstNode,
                                                            EntityChannelSpark second,
                                                            IGridNode secondNode) {
        if (isCableNode(firstNode)) {
            return getCableAttachmentDirection(first);
        }
        if (isCableNode(secondNode)) {
            AEPartLocation direction = getCableAttachmentDirection(second);
            return direction == AEPartLocation.INTERNAL
                    ? AEPartLocation.INTERNAL : direction.getOpposite();
        }
        return AEPartLocation.INTERNAL;
    }

    private static AEPartLocation getCableAttachmentDirection(EntityChannelSpark spark) {
        if (spark == null || spark.getTargetPos() == null) {
            return AEPartLocation.UP;
        }

        BlockPos target = spark.getTargetPos();
        double dx = spark.posX - (target.getX() + 0.5D);
        double dy = spark.posY - (target.getY() + 0.5D);
        double dz = spark.posZ - (target.getZ() + 0.5D);
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        if (ay >= ax && ay >= az) {
            return dy >= 0.0D ? AEPartLocation.UP : AEPartLocation.DOWN;
        }
        if (ax >= az) {
            return dx >= 0.0D ? AEPartLocation.EAST : AEPartLocation.WEST;
        }
        return dz >= 0.0D ? AEPartLocation.SOUTH : AEPartLocation.NORTH;
    }

    private static void downgradeConnection(EntityChannelSpark first, EntityChannelSpark second,
                                             Object connection) {
        Link firstLink = first.getLinks().get(second.getUniqueID());
        if (firstLink == null || firstLink.connection != connection) {
            return;
        }
        destroyConnection(connection);
        first.getLinks().put(second.getUniqueID(), new Link(second, null));
        second.getLinks().put(first.getUniqueID(), new Link(first, null));
    }

    private static void unlink(EntityChannelSpark first, EntityChannelSpark second, Object connection) {
        if (first != null) {
            first.getLinks().remove(second == null ? null : second.getUniqueID());
        }
        if (second != null) {
            second.getLinks().remove(first == null ? null : first.getUniqueID());
        }
        destroyConnection(connection);
    }

    /**
     * Finds the active AE2 node exposed by a block or a part on a cable bus.
     * The reflection fallback covers AE2, AE2FC and Mekanism Energistics
     * hosts without hard-linking this feature to any one implementation.
     */
    public static IGridNode findGridNode(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return null;
        }

        // AE2 cable buses and network tiles expose their active node through
        // IGridHost. Prefer this public API so an ME interface/part is not
        // mistaken for a visual or non-carrying cable-bus node.
        if (tile instanceof IGridHost) {
            IGridNode hostNode = findGridNodeOnHost((IGridHost) tile);
            if (isUsable(hostNode)) {
                return hostNode;
            }
        }

        IGridNode node = findGridNodeOnObject(tile);
        if (isUsable(node)) {
            return node;
        }

        if (tile instanceof IPartHost) {
            IPartHost partHost = (IPartHost) tile;
            for (AEPartLocation location : AEPartLocation.values()) {
                IPart part = partHost.getPart(location);
                node = findGridNodeOnObject(part);
                if (isUsable(node)) {
                    return node;
                }
            }
        }

        return null;
    }

    /**
     * Resolves a node even when AE2 currently reports it as inactive. An
     * interface can be inactive because it does not have a channel yet, while
     * its cable-bus node is still the correct place to create the bridge.
     */
    private static IGridNode findAnyGridNode(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return null;
        }

        if (tile instanceof IGridHost) {
            IGridNode node = findAnyGridNodeOnHost((IGridHost) tile);
            if (node != null) {
                return node;
            }
        }

        IGridNode node = findGridNodeOnObject(tile, false);
        if (node != null) {
            return node;
        }

        if (tile instanceof IPartHost) {
            IPartHost partHost = (IPartHost) tile;
            for (AEPartLocation location : AEPartLocation.values()) {
                IPart part = partHost.getPart(location);
                node = findGridNodeOnObject(part, false);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Returns a node that can actually carry an AE2 path.  Machine/interface
     * nodes are valid endpoints for devices, but AE2 deliberately gives them
     * CANNOT_CARRY and they cannot be used as the two ends of a channel bridge.
     * For cable buses, use the center/carrying node first and only fall back to
     * a host node when no carrying node is exposed.
     */
    /**
     * Gets all channel-bearing nodes exposed by the block below a spark.
     * Controller faces are collected in +X,+Y,+Z,-X,-Y,-Z order. Cable buses
     * collapse to one carrying node because all of their faces share a path.
     */
    private static List<BridgeEndpoint> findBridgeEndpoints(World world, BlockPos pos) {
        List<BridgeEndpoint> endpoints = new ArrayList<>();
        if (world == null || pos == null) {
            return endpoints;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IGridHost) {
            IGridHost host = (IGridHost) tile;
            boolean controller = host instanceof TileController;
            for (EnumFacing facing : CHANNEL_DIRECTION_ORDER) {
                AEPartLocation direction = AEPartLocation.fromFacing(facing);
                IGridNode node = null;
                try {
                    node = host.getGridNode(direction);
                } catch (Throwable ignored) {
                }
                addBridgeEndpoint(endpoints, node, direction, controller);
            }

            if (endpoints.isEmpty()) {
                IGridNode internal = null;
                try {
                    internal = host.getGridNode(AEPartLocation.INTERNAL);
                } catch (Throwable ignored) {
                }
                addBridgeEndpoint(endpoints, internal, AEPartLocation.INTERNAL, controller);
            }
            if (!endpoints.isEmpty()) {
                return collapseCableEndpoints(endpoints);
            }
        } else {
            addBridgeEndpoint(endpoints, findGridNodeOnObject(tile, false),
                    AEPartLocation.INTERNAL);
        }

        // Keep the adjacent-cable fallback for an ordinary block without a
        // usable host node, using the same deterministic face order.
        for (EnumFacing facing : CHANNEL_DIRECTION_ORDER) {
            TileEntity adjacent = world.getTileEntity(pos.offset(facing));
            addBridgeEndpoint(endpoints, findGridNodeOnObject(adjacent, false),
                    AEPartLocation.fromFacing(facing));
        }
        return collapseCableEndpoints(endpoints);
    }

    private static List<BridgeEndpoint> collapseCableEndpoints(
            List<BridgeEndpoint> endpoints) {
        BridgeEndpoint firstCable = null;
        for (BridgeEndpoint endpoint : endpoints) {
            if (isCableNode(endpoint.node)) {
                firstCable = endpoint;
                break;
            }
        }
        if (firstCable == null) {
            return endpoints;
        }
        return Collections.singletonList(
                new BridgeEndpoint(firstCable.node, AEPartLocation.INTERNAL));
    }

    private static void addBridgeEndpoint(List<BridgeEndpoint> endpoints,
                                          IGridNode node,
                                          AEPartLocation direction) {
        addBridgeEndpoint(endpoints, node, direction, false);
    }

    private static void addBridgeEndpoint(List<BridgeEndpoint> endpoints,
                                          IGridNode node,
                                          AEPartLocation direction,
                                          boolean controllerFace) {
        if (controllerFace ? !isControllerNode(node) : !isCarryingNode(node)) {
            return;
        }
        for (BridgeEndpoint existing : endpoints) {
            if (existing.node == node
                    && (!controllerFace || (existing.controllerFace
                    && existing.direction == direction))) {
                return;
            }
        }
        endpoints.add(new BridgeEndpoint(node, direction, controllerFace));
    }

    /**
     * AE2 controllers expose a dense channel allocator while also marking
     * their node CANNOT_CARRY. That flag is correct for ordinary path nodes,
     * but must not reject a controller face used as a bridge endpoint.
     */
    private static boolean isControllerNode(IGridNode node) {
        try {
            return node != null && node.getGrid() != null
                    && node.hasFlag(GridFlags.DENSE_CAPACITY);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static IGridNode findBridgeNode(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return null;
        }

        // Never select an arbitrary carrying node from the complete AE2 grid.
        // A direct wireless connection has no cable side/facing; attaching it
        // to an unrelated dense cable can make AE2 pass a null facing into its
        // collision-box code. The target block's internal node is the stable
        // bridge point for a cable bus and preserves the player's placement.
        IGridNode endpoint = tile instanceof IGridHost
                ? findCarryingGridNodeOnHost((IGridHost) tile)
                : findGridNodeOnObject(tile, false);
        IGridNode bridgeNode = selectBridgeNode(endpoint);
        if (bridgeNode != null) {
            return bridgeNode;
        }

        // If the clicked block is a machine/interface node that cannot carry
        // a path itself, accept only a directly adjacent cable bus. This keeps
        // the bridge local and avoids changing an unrelated part of the grid.
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity adjacent = world.getTileEntity(pos.offset(facing));
            if (adjacent == null) {
                continue;
            }
            endpoint = adjacent instanceof IGridHost
                    ? findCarryingGridNodeOnHost((IGridHost) adjacent)
                    : findGridNodeOnObject(adjacent, false);
            bridgeNode = selectBridgeNode(endpoint);
            if (bridgeNode != null) {
                return bridgeNode;
            }
        }
        return null;
    }

    private static IGridNode findCarryingNodeInNetwork(IGridNode endpoint) {
        if (endpoint == null) {
            return null;
        }

        // The endpoint may be an inactive CANNOT_CARRY interface node. Its
        // local connection collection is not reliable while AE2 is rebuilding
        // channels, but the owning grid still exposes all of its nodes.
        IGridNode gridCarrying = findCarryingNodeFromGrid(endpoint);
        if (gridCarrying != null) {
            return gridCarrying;
        }

        ArrayDeque<IGridNode> queue = new ArrayDeque<>();
        Set<IGridNode> visited = Collections.newSetFromMap(
                new IdentityHashMap<IGridNode, Boolean>());
        queue.add(endpoint);
        visited.add(endpoint);

        // A malformed or unusually large network must not make every spark
        // tick scan without bounds. The first carrying node is normally the
        // adjacent cable-bus node, so this limit is only a safety net.
        int scanned = 0;
        IGridNode fallback = null;
        IGridNode restrictedFallback = null;
        while (!queue.isEmpty() && scanned++ < 4096) {
            IGridNode node = queue.removeFirst();
            if (isCarryingNode(node)) {
                if (isPreferredCarryingNode(node)) {
                    if (hasDenseCapacity(node)) {
                        return node;
                    }
                    if (fallback == null) {
                        fallback = node;
                    }
                } else if (restrictedFallback == null) {
                    restrictedFallback = node;
                }
            }

            try {
                for (IGridConnection connection : node.getConnections()) {
                    if (connection == null) {
                        continue;
                    }
                    IGridNode other;
                    try {
                        other = connection.getOtherSide(node);
                    } catch (Throwable ignored) {
                        continue;
                    }
                    if (other != null && visited.add(other)) {
                        queue.addLast(other);
                    }
                }
            } catch (Throwable ignored) {
                // A grid can be rebuilding while a spark is ticking.
            }
        }
        return fallback == null ? restrictedFallback : fallback;
    }

    /**
     * Finds a dense carrying node from the complete AE grid when possible.
     * A dense node is preferred because the wireless bridge is intended to
     * carry the configured 32-channel capacity; a normal carrying node is a
     * safe fallback for networks that do not contain dense cabling.
     */
    private static IGridNode findCarryingNodeFromGrid(IGridNode endpoint) {
        IGridNode fallback = null;
        IGridNode restrictedFallback = null;
        try {
            if (endpoint.getGrid() == null) {
                return null;
            }

            // IGrid.getNodes() is part of AE2's public API. Use it directly
            // instead of reflecting on the concrete Grid implementation; the
            // latter can silently fail on Extended Life or other AE2 builds.
            for (IGridNode candidate : endpoint.getGrid().getNodes()) {
                if (!isCarryingNode(candidate)) {
                    continue;
                }
                if (isPreferredCarryingNode(candidate)) {
                    if (hasDenseCapacity(candidate)) {
                        return candidate;
                    }
                    if (fallback == null) {
                        fallback = candidate;
                    }
                } else if (restrictedFallback == null) {
                    restrictedFallback = candidate;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback == null ? restrictedFallback : fallback;
    }

    private static boolean hasDenseCapacity(IGridNode node) {
        try {
            return isCarryingNode(node) && node.hasFlag(GridFlags.DENSE_CAPACITY);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Prefer a cable/path node that does not itself consume a channel. An AE2
     * device can technically carry a route, but choosing one as the bridge
     * endpoint can make the first channel allocation depend on that device's
     * own channel requirement and leave the wireless link unusable.
     */
    private static boolean isPreferredCarryingNode(IGridNode node) {
        try {
            return isCarryingNode(node)
                    && !node.hasFlag(GridFlags.REQUIRE_CHANNEL)
                    && !node.hasFlag(GridFlags.CANNOT_CARRY_COMPRESSED);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isCarryingNode(IGridNode node) {
        try {
            // Do not require isActive() here. A network may report a machine
            // as inactive precisely because it is waiting for a channel; the
            // wireless bridge must be allowed to connect before AE2 can
            // recalculate that channel state. The node must already belong to
            // an AE grid and must be able to carry a route.
            return node != null && node.getGrid() != null
                    && !node.hasFlag(GridFlags.CANNOT_CARRY);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static IGridNode findGridNodeOnObject(Object object) {
        return findGridNodeOnObject(object, true);
    }

    private static IGridNode findGridNodeOnObject(Object object, boolean requireActive) {
        if (object == null) {
            return null;
        }

        if (object instanceof IGridNode) {
            IGridNode node = (IGridNode) object;
            if (isAccepted(node, requireActive)) {
                return node;
            }
        }

        if (object instanceof IGridHost) {
            IGridNode hostNode = requireActive
                    ? findGridNodeOnHost((IGridHost) object)
                    : findAnyGridNodeOnHost((IGridHost) object);
            if (isAccepted(hostNode, requireActive)) {
                return hostNode;
            }
        }

        if (object instanceof IPart) {
            IGridNode partNode = ((IPart) object).getGridNode();
            if (isAccepted(partNode, requireActive)) {
                return partNode;
            }
        }

        if (object instanceof IPartHost) {
            IPartHost partHost = (IPartHost) object;
            for (AEPartLocation location : AEPartLocation.values()) {
                IPart part = partHost.getPart(location);
                if (part == null) {
                    continue;
                }
                IGridNode partNode = part.getGridNode();
                if (isAccepted(partNode, requireActive)) {
                    return partNode;
                }
            }
        }

        return null;
    }

    private static boolean isAccepted(IGridNode node, boolean requireActive) {
        return node != null && (!requireActive || isUsable(node));
    }

    private static IGridNode findAnyGridNodeOnHost(IGridHost host) {
        try {
            IGridNode internal = host.getGridNode(AEPartLocation.INTERNAL);
            if (internal != null) {
                return internal;
            }
        } catch (Throwable ignored) {
        }
        for (AEPartLocation location : AEPartLocation.values()) {
            if (location == AEPartLocation.INTERNAL) {
                continue;
            }
            try {
                IGridNode node = host.getGridNode(location);
                if (node != null) {
                    return node;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static IGridNode findGridNodeOnHost(IGridHost host) {
        try {
            IGridNode internal = host.getGridNode(AEPartLocation.INTERNAL);
            if (isUsable(internal)) {
                return internal;
            }
        } catch (Throwable ignored) {
        }
        for (AEPartLocation location : AEPartLocation.values()) {
            if (location == AEPartLocation.INTERNAL) {
                continue;
            }
            try {
                IGridNode node = host.getGridNode(location);
                if (isUsable(node)) {
                    return node;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static IGridNode findCarryingGridNodeOnHost(IGridHost host) {
        if (host == null) {
            return null;
        }
        try {
            IGridNode internal = host.getGridNode(AEPartLocation.INTERNAL);
            IGridNode bridgeNode = selectBridgeNode(internal);
            if (bridgeNode != null) {
                return bridgeNode;
            }
        } catch (Throwable ignored) {
        }
        // A side node has a meaningful direction only when it belongs to the
        // part that owns that side. A wireless bridge has no physical side,
        // so using one here can make AE2 calculate a null direction while it
        // builds cable collision boxes. Only the host's internal node is safe.
        return null;
    }

    /**
     * Selects a safe AE2 endpoint for a wireless bridge. Cable nodes expose
     * their connections as physical sides. Adding an INTERNAL GridConnection
     * to one of those nodes makes PartDenseCable treat INTERNAL as a side and
     * eventually call getFacing() on null while building collision boxes.
     * Use another carrying node from the same grid instead; it preserves the
     * network without changing any cable's physical connection set.
     */
    private static IGridNode selectBridgeNode(IGridNode endpoint) {
        if (!isCarryingNode(endpoint)) {
            return null;
        }
        // Keep the cable node as the endpoint so AE2's channel path reaches
        // smart/dense cable accounting. The connection factory supplies a
        // real physical direction for cable endpoints instead of INTERNAL.
        return endpoint;
    }

    private static IGridNode findNonCableCarryingNodeFromGrid(IGridNode endpoint) {
        IGridNode fallback = null;
        try {
            if (endpoint.getGrid() == null) {
                return null;
            }
            for (IGridNode candidate : endpoint.getGrid().getNodes()) {
                if (!isCarryingNode(candidate) || isCableNode(candidate)) {
                    continue;
                }
                if (isPreferredCarryingNode(candidate)) {
                    return candidate;
                }
                if (fallback == null) {
                    fallback = candidate;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static boolean isCableNode(IGridNode node) {
        if (node == null) {
            return false;
        }
        try {
            Object machine = node.getGridBlock().getMachine();
            if (machine == null) {
                return false;
            }
            String name = machine.getClass().getName();
            return machine instanceof appeng.tile.networking.TileCableBus
                    || name.contains("CableBus")
                    || name.contains("Cable");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isUsable(IGridNode node) {
        if (node == null) {
            return false;
        }
        try {
            if (!node.isActive()) {
                return false;
            }
            // A channel spark is an external bridge.  The endpoint itself may
            // be a CANNOT_CARRY machine node (for example an ME interface),
            // but it must still be a live AE node.  Rejecting that flag here
            // prevents the spark bridge from ever reaching the ME network.
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createGridConnection(IGridNode first, IGridNode second,
                                               AEPartLocation direction) {
        // Use AE2's public concrete factory so the requested part direction is
        // preserved without reflective lookup. This is important for cable
        // geometry and channel accounting.
        PENDING_CHANNEL_CAPACITY.set(ChannelSparkConfig.getChannelCapacity());
        try {
            IGridConnection connection = GridConnection.create(first, second, direction);
            registerConnection(connection);
            repathAfterConnection(first, second);
            return connection;
        } catch (Throwable error) {
            logConnectionFailure("GridConnection public factory", first, second, error);
        } finally {
            PENDING_CHANNEL_CAPACITY.remove();
        }

        // AEApi's convenience method creates an INTERNAL connection on this
        // AE2 build. It is safe only when no cable endpoint needs a physical
        // direction; using it for a cable would reintroduce a null facing in
        // PartDenseCable.isDense().
        if (direction != AEPartLocation.INTERNAL) {
            return null;
        }

        try {
            PENDING_CHANNEL_CAPACITY.set(ChannelSparkConfig.getChannelCapacity());
            try {
                IGridConnection connection = AEApi.instance().grid().createGridConnection(first, second);
                registerConnection(connection);
                repathAfterConnection(first, second);
                return connection;
            } finally {
                PENDING_CHANNEL_CAPACITY.remove();
            }
        } catch (Throwable error) {
            logConnectionFailure("AE API", first, second, error);
        }
        return null;
    }

    private static void logConnectionFailure(String mechanism, IGridNode first,
                                              IGridNode second, Throwable error) {
        try {
            if (BotaniaApplie.getLogger() != null && BotaniaApplie.getLogger().isDebugEnabled()) {
                BotaniaApplie.getLogger().debug(
                        "Channel spark AE bridge creation failed via " + mechanism
                                + " (" + describeNode(first) + " -> " + describeNode(second) + ")",
                        error);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String describeNode(IGridNode node) {
        if (node == null) {
            return "null";
        }
        try {
            return node.getGridBlock().getMachine().getClass().getName();
        } catch (Throwable ignored) {
            return node.getClass().getName();
        }
    }

    /**
     * AE2's GridConnection factory schedules its path rebuild before it adds
     * the newly-created connection to both endpoint nodes. Rebuild once more
     * after the factory returns so channel consumers can see this wireless
     * bridge immediately instead of waiting for an unrelated grid change.
     */
    private static void repathAfterConnection(IGridNode first, IGridNode second) {
        try {
            if (first != null && first.getGrid() != null) {
                IPathingGrid pathing = (IPathingGrid) first.getGrid().getCache(IPathingGrid.class);
                pathing.repath();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (second != null && second.getGrid() != null
                    && (first == null || second.getGrid() != first.getGrid())) {
                IPathingGrid pathing = (IPathingGrid) second.getGrid().getCache(IPathingGrid.class);
                pathing.repath();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void destroyConnection(Object connection) {
        if (!(connection instanceof IGridConnection)) {
            return;
        }
        IGridConnection gridConnection = (IGridConnection) connection;
        IGridNode first = null;
        IGridNode second = null;
        try {
            first = gridConnection.a();
            second = gridConnection.b();
        } catch (Throwable ignored) {
        }
        try {
            gridConnection.destroy();
        } catch (Throwable ignored) {
            return;
        }
        WIRELESS_CONNECTIONS.remove(gridConnection);
        CHANNEL_CAPACITIES.remove(gridConnection);
        repathAfterConnection(first, second);
    }
}
