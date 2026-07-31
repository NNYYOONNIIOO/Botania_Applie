package nyonio;

public interface IFluxExtendedUpgradeInventory {
    int flux_applied$getInstalledUpgrades(IFluxUpgradeModule upgrade);
    int flux_applied$getMaxInstalled(IFluxUpgradeModule upgrade);
    boolean flux_applied$isInterfaceDevice();
}
