package nyonio.integration.terminal;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import nyonio.ae2.ManaStorageChannel;
import nyonio.terminal_interaction_integration.api.IContainerHandler;
import nyonio.terminal_interaction_integration.api.IPacketType;
import nyonio.terminal_interaction_integration.api.IResourceProvider;

public class ManaResourceProvider implements IResourceProvider {
    
    private final ManaPacketType packetType;
    private final ManaContainerHandler containerHandler;
    
    public ManaResourceProvider() {
        this.packetType = new ManaPacketType();
        this.containerHandler = new ManaContainerHandler();
    }
    
    @Override
    public String getName() {
        return "mana";
    }
    
    @Override
    public IStorageChannel<? extends IAEStack<?>> getStorageChannel() {
        return ManaStorageChannel.INSTANCE;
    }
    
    @Override
    public IPacketType getPacketType() {
        return packetType;
    }
    
    @Override
    public IContainerHandler getContainerHandler() {
        return containerHandler;
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}
