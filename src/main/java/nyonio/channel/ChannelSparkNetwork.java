package nyonio.channel;

import appeng.api.AEApi;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.GridFlags;
import appeng.api.util.AEPartLocation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
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
            if (existing.connection == null) {
                Object connection = createGridConnectionIfPossible(first, second);
                if (connection != null) {
                    first.getLinks().put(second.getUniqueID(), new Link(second, connection));
                    second.getLinks().put(first.getUniqueID(), new Link(first, connection));
                }
            }
            return;
        }

        // The logical spark link is created even when one or both sparks are
        // floating. It is upgraded to an AE connection as soon as both ends
        // expose usable grid nodes.
        Object connection = createGridConnectionIfPossible(first, second);
        first.getLinks().put(second.getUniqueID(), new Link(second, connection));
        second.getLinks().put(first.getUniqueID(), new Link(first, connection));
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

        return createGridConnection(firstNode, secondNode);
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

        // A machine such as an ME interface exposes a live node, but that
        // node is marked CANNOT_CARRY by AE2. Walk its existing AE graph to
        // find the cable-bus node that can actually carry the bridge channel.
        IGridNode endpoint = findGridNode(world, pos);
        if (endpoint == null) {
            endpoint = findAnyGridNode(world, pos);
        }
        IGridNode carrying = findCarryingNodeInNetwork(endpoint);
        if (carrying != null) {
            return carrying;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            return null;
        }

        for (EnumFacing facing : EnumFacing.values()) {
            try {
                Method getPart = tile.getClass().getMethod("getPart", EnumFacing.class);
                Object part = getPart.invoke(tile, facing);
                endpoint = findGridNodeOnObject(part, false);
                carrying = findCarryingNodeInNetwork(endpoint);
                if (carrying != null) {
                    return carrying;
                }
            } catch (Throwable ignored) {
            }
        }

        for (AEPartLocation location : AEPartLocation.values()) {
            try {
                Method getPart = tile.getClass().getMethod("getPart", AEPartLocation.class);
                Object part = getPart.invoke(tile, location);
                endpoint = findGridNodeOnObject(part, false);
                carrying = findCarryingNodeInNetwork(endpoint);
                if (carrying != null) {
                    return carrying;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static IGridNode findCarryingNodeInNetwork(IGridNode endpoint) {
        if (endpoint == null) {
            return null;
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
        while (!queue.isEmpty() && scanned++ < 4096) {
            IGridNode node = queue.removeFirst();
            if (isCarryingNode(node)) {
                return node;
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
        return null;
    }

    private static boolean isCarryingNode(IGridNode node) {
        try {
            return node != null && node.isActive()
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
        for (AEPartLocation location : AEPartLocation.values()) {
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
        for (AEPartLocation location : AEPartLocation.values()) {
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

    private static Object createGridConnection(IGridNode first, IGridNode second) {
        try {
            PENDING_CHANNEL_CAPACITY.set(ChannelSparkConfig.getChannelCapacity());
            try {
                IGridConnection connection = AEApi.instance().grid().createGridConnection(first, second);
                registerConnection(connection);
                return connection;
            } finally {
                PENDING_CHANNEL_CAPACITY.remove();
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> connectionClass = Class.forName(GRID_CONNECTION_CLASS);
            Method factory = connectionClass.getDeclaredMethod("create", IGridNode.class, IGridNode.class, AEPartLocation.class);
            if (!factory.isAccessible()) {
                factory.setAccessible(true);
            }
            PENDING_CHANNEL_CAPACITY.set(ChannelSparkConfig.getChannelCapacity());
            try {
                Object connection = factory.invoke(null, first, second, AEPartLocation.INTERNAL);
                registerConnection(connection);
                return connection;
            } finally {
                PENDING_CHANNEL_CAPACITY.remove();
            }
        } catch (Throwable ignored) {
        }
        return null;
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
        for (String name : new String[]{"destroy", "disconnect", "close"}) {
            try {
                Method method = connection.getClass().getMethod(name);
                if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 0) {
                    method.invoke(connection);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
