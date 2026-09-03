package nyonio.channel;

import appeng.api.AEApi;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.GridFlags;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.util.AEPartLocation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
import nyonio.ChannelSparkConfig;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.common.network.PacketBotaniaEffect;
import vazkii.botania.common.network.PacketHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
 * AE2's concrete GridConnection implementation is deliberately used through
 * reflection.  This keeps the mod compatible with the AE2 Extended Life
 * builds used by the project while still allowing the link to be a dense
 * (32-channel) connection when the implementation exposes that constructor.
 */
public final class ChannelSparkNetwork {
    private static final String GRID_CONNECTION_CLASS = "appeng.me.GridConnection";
    private static final Map<IGridConnection, Integer> CHANNEL_CAPACITIES =
            Collections.synchronizedMap(new WeakHashMap<IGridConnection, Integer>());
    private static final Map<BridgeKey, BridgeRecord> AE_BRIDGES = new HashMap<>();
    private static final ThreadLocal<Integer> PENDING_CHANNEL_CAPACITY = new ThreadLocal<>();

    private ChannelSparkNetwork() {
    }

    public static Integer getCapacity(IGridConnection connection) {
        Integer capacity = connection == null ? null : CHANNEL_CAPACITIES.get(connection);
        return capacity == null ? PENDING_CHANNEL_CAPACITY.get() : capacity;
    }

    private static void registerConnection(Object connection) {
        if (connection instanceof IGridConnection) {
            CHANNEL_CAPACITIES.put((IGridConnection) connection, ChannelSparkConfig.getChannelCapacity());
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

    private static final class BridgeRecord {
        private final EntityChannelSpark first;
        private final EntityChannelSpark second;
        private final IGridConnection connection;

        private BridgeRecord(EntityChannelSpark first, EntityChannelSpark second,
                             IGridConnection connection) {
            this.first = first;
            this.second = second;
            this.connection = connection;
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
                && findBridgeNode(spark.world, spark.getTargetPos()) != null;
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
        List<EntityChannelSpark> endpointSparks = new ArrayList<>();
        for (EntityChannelSpark spark : network) {
            if (spark.hasTarget() && findBridgeNode(spark.world, spark.getTargetPos()) != null) {
                endpointSparks.add(spark);
            }
        }
        if (endpointSparks.size() < 2) {
            return;
        }

        EntityChannelSpark anchor = endpointSparks.get(0);
        for (int index = 1; index < endpointSparks.size(); index++) {
            EntityChannelSpark other = endpointSparks.get(index);
            BridgeKey key = new BridgeKey(anchor.getUniqueID(), other.getUniqueID());
            BridgeRecord existing = AE_BRIDGES.get(key);
            if (existing != null) {
                if (isConnectionAlive(existing.connection)) {
                    continue;
                }
                AE_BRIDGES.remove(key);
            }

            Object connection = createGridConnectionIfPossible(anchor, other);
            if (connection instanceof IGridConnection) {
                AE_BRIDGES.put(key, new BridgeRecord(anchor, other,
                        (IGridConnection) connection));
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
                    && isConnectionAlive(record.connection);
            if (!valid) {
                destroyConnection(record.connection);
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
                destroyConnection(record.connection);
                iterator.remove();
            }
        }
    }

    private static Object createGridConnectionIfPossible(EntityChannelSpark first, EntityChannelSpark second) {
        if (!hasUsableEndpoint(first) || !hasUsableEndpoint(second)) {
            return null;
        }

        IGridNode firstNode = findBridgeNode(first.world, first.getTargetPos());
        IGridNode secondNode = findBridgeNode(second.world, second.getTargetPos());
        if (firstNode == null || secondNode == null || firstNode == secondNode) {
            return null;
        }

        try {
            if (firstNode.getGrid() != null && firstNode.getGrid() == secondNode.getGrid()) {
                return null;
            }
        } catch (Throwable ignored) {
        }

        // A spark bridge is not a physical cable side. Keep the connection
        // internal so AE2 still counts its channels while the dense-cable
        // geometry mixin prevents INTERNAL from being treated as a direction.
        return createGridConnection(firstNode, secondNode, AEPartLocation.INTERNAL);
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

        try {
            Method getPart = tile.getClass().getMethod("getPart", EnumFacing.class);
            for (EnumFacing facing : EnumFacing.values()) {
                Object part = getPart.invoke(tile, facing);
                node = findGridNodeOnObject(part);
                if (isUsable(node)) {
                    return node;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Method getPart = tile.getClass().getMethod("getPart", AEPartLocation.class);
            for (AEPartLocation location : AEPartLocation.SIDE_LOCATIONS) {
                Object part = getPart.invoke(tile, location);
                node = findGridNodeOnObject(part);
                if (isUsable(node)) {
                    return node;
                }
            }
        } catch (Throwable ignored) {
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

        try {
            Method getPart = tile.getClass().getMethod("getPart", EnumFacing.class);
            for (EnumFacing facing : EnumFacing.values()) {
                Object part = getPart.invoke(tile, facing);
                node = findGridNodeOnObject(part, false);
                if (node != null) {
                    return node;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Method getPart = tile.getClass().getMethod("getPart", AEPartLocation.class);
            for (AEPartLocation location : AEPartLocation.SIDE_LOCATIONS) {
                Object part = getPart.invoke(tile, location);
                node = findGridNodeOnObject(part, false);
                if (node != null) {
                    return node;
                }
            }
        } catch (Throwable ignored) {
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

        if (object instanceof IGridHost) {
            IGridNode hostNode = requireActive
                    ? findGridNodeOnHost((IGridHost) object)
                    : findAnyGridNodeOnHost((IGridHost) object);
            if (isAccepted(hostNode, requireActive)) {
                return hostNode;
            }
        }

        try {
            Method getProxy = findMethod(object.getClass(), "getProxy");
            if (getProxy == null) {
                throw new NoSuchMethodException("getProxy");
            }
            getProxy.setAccessible(true);
            Object proxy = getProxy.invoke(object);
            if (proxy != null) {
                Method getNode = findMethod(proxy.getClass(), "getNode");
                if (getNode == null) {
                    throw new NoSuchMethodException("getNode");
                }
                getNode.setAccessible(true);
                Object node = getNode.invoke(proxy);
                if (node instanceof IGridNode && isAccepted((IGridNode) node, requireActive)) {
                    return (IGridNode) node;
                }
            }
        } catch (Throwable ignored) {
        }

        for (Method method : object.getClass().getMethods()) {
            if (!"getGridNode".equals(method.getName())) {
                continue;
            }
            try {
                method.setAccessible(true);
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    Object node = method.invoke(object);
                    if (node instanceof IGridNode && isAccepted((IGridNode) node, requireActive)) {
                        return (IGridNode) node;
                    }
                    continue;
                }
                if (parameterTypes.length != 1) {
                    continue;
                }
                Class<?> parameter = parameterTypes[0];
                if (parameter.isEnum()) {
                    Object[] values = parameter.getEnumConstants();
                    for (Object value : values) {
                        Object node = method.invoke(object, value);
                        if (node instanceof IGridNode && isAccepted((IGridNode) node, requireActive)) {
                            return (IGridNode) node;
                        }
                    }
                } else {
                    Object node = method.invoke(object, new Object[]{null});
                    if (node instanceof IGridNode && isAccepted((IGridNode) node, requireActive)) {
                        return (IGridNode) node;
                    }
                }
            } catch (Throwable ignored) {
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

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
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
        // AEApi's convenience method does not preserve an explicit part
        // location on every AE2 Extended Life build. When it creates a
        // connection without a location, dense-cable inspection can receive a
        // null facing and crash while TOP is probing the cable. Prefer the
        // concrete factory with INTERNAL for both wireless endpoints.
        try {
            Class<?> connectionClass = Class.forName(GRID_CONNECTION_CLASS);
            Method factory = connectionClass.getDeclaredMethod("create", IGridNode.class, IGridNode.class, AEPartLocation.class);
            if (!factory.isAccessible()) {
                factory.setAccessible(true);
            }
            PENDING_CHANNEL_CAPACITY.set(ChannelSparkConfig.getChannelCapacity());
            try {
                Object connection = factory.invoke(null, first, second, direction);
                registerConnection(connection);
                repathAfterConnection(first, second);
                return connection;
            } finally {
                PENDING_CHANNEL_CAPACITY.remove();
            }
        } catch (Throwable error) {
            logConnectionFailure("GridConnection internal factory", first, second, error);
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

    private static Object[] buildArguments(Class<?>[] parameterTypes, IGridNode first,
                                            IGridNode second, boolean dense) {
        Object[] arguments = new Object[parameterTypes.length];
        int nodeIndex = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameter = parameterTypes[i];
            if (nodeIndex < 2 && isNodeParameter(parameter, first)) {
                arguments[i] = nodeIndex++ == 0 ? first : second;
            } else if (parameter == boolean.class || parameter == Boolean.class) {
                arguments[i] = dense;
            } else if (parameter == int.class || parameter == Integer.class) {
                arguments[i] = ChannelSparkConfig.getChannelCapacity();
            } else if (parameter == long.class || parameter == Long.class) {
                arguments[i] = (long) ChannelSparkConfig.getChannelCapacity();
            } else if (EnumSet.class.isAssignableFrom(parameter) || Set.class.isAssignableFrom(parameter)) {
                Object flags = createDenseFlags();
                if (flags == null) {
                    return null;
                }
                arguments[i] = flags;
            } else if (parameter.isEnum()) {
                Object flag = findDenseFlag(parameter);
                if (flag == null) {
                    return null;
                }
                arguments[i] = flag;
            } else {
                return null;
            }
        }
        return nodeIndex == 2 ? arguments : null;
    }

    private static boolean isNodeParameter(Class<?> parameter, IGridNode node) {
        return parameter != Object.class
                && (IGridNode.class.isAssignableFrom(parameter) || parameter.isInstance(node));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object createDenseFlags() {
        try {
            Class<?> flagsClass = Class.forName("appeng.api.networking.GridFlags");
            Object dense = Enum.valueOf((Class<? extends Enum>) flagsClass, "DENSE_CAPACITY");
            return EnumSet.of((Enum) dense);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object findDenseFlag(Class<?> enumClass) {
        try {
            return Enum.valueOf((Class<? extends Enum>) enumClass, "DENSE_CAPACITY");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void destroyConnection(Object connection) {
        if (connection == null) {
            return;
        }
        IGridNode first = null;
        IGridNode second = null;
        if (connection instanceof IGridConnection) {
            try {
                first = ((IGridConnection) connection).a();
                second = ((IGridConnection) connection).b();
            } catch (Throwable ignored) {
            }
        }
        boolean destroyed = false;
        for (String name : new String[]{"destroy", "disconnect", "close"}) {
            try {
                Method method = connection.getClass().getMethod(name);
                if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 0) {
                    method.invoke(connection);
                    destroyed = true;
                    break;
                }
            } catch (Throwable ignored) {
            }
        }
        if (destroyed) {
            repathAfterConnection(first, second);
        }
    }
}
