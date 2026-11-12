// One-time mechanical 1.12 -> 1.16 migration; semantic adaptations are reviewed separately.
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..');
const walk = dir => fs.readdirSync(dir, {withFileTypes:true}).flatMap(e => e.isDirectory() ? walk(path.join(dir,e.name)) : [path.join(dir,e.name)]);
const tokens = {
  EntityPlayerMP:'ServerPlayerEntity', EntityPlayer:'PlayerEntity', EntityItem:'ItemEntity', WorldServer:'ServerWorld',
  NBTTagCompound:'CompoundNBT', NBTTagList:'ListNBT', EnumFacing:'Direction', EnumHand:'Hand', EnumActionResult:'ActionResultType',
  EnumDyeColor:'DyeColor', IBlockState:'BlockState', Vec3d:'Vector3d', RayTraceResult:'RayTraceResult', CreativeTabs:'ItemGroup',
  TileController:'ControllerTileEntity', TileCableBus:'CableBusTileEntity', TileDrive:'DriveTileEntity',
  ContainerMEMonitorable:'MEMonitorableContainer', PartDenseCable:'DenseCablePart', PartCable:'CablePart',
  SideOnly:'OnlyIn', Side:'Dist', AEApi:'Api'
};
const full = {
 'net.minecraft.util.math.Vec3d':'net.minecraft.util.math.vector.Vector3d',
 'net.minecraft.util.math.Vector3d':'net.minecraft.util.math.vector.Vector3d',
 'net.minecraft.world.WorldServer':'net.minecraft.world.server.ServerWorld',
 'net.minecraft.world.ServerWorld':'net.minecraft.world.server.ServerWorld',
 'net.minecraft.block.state.IBlockState':'net.minecraft.block.BlockState',
 'net.minecraft.block.state.BlockState':'net.minecraft.block.BlockState',
 'net.minecraft.creativetab.CreativeTabs':'net.minecraft.item.ItemGroup',
 'net.minecraft.creativetab.ItemGroup':'net.minecraft.item.ItemGroup',
 'net.minecraftforge.fml.common.eventhandler.SubscribeEvent':'net.minecraftforge.eventbus.api.SubscribeEvent',
 'net.minecraftforge.fml.common.gameevent.TickEvent':'net.minecraftforge.event.TickEvent',
 'net.minecraftforge.fml.relauncher.SideOnly':'net.minecraftforge.api.distmarker.OnlyIn',
 'net.minecraftforge.fml.relauncher.Side':'net.minecraftforge.api.distmarker.Dist',
 'net.minecraftforge.fml.relauncher.OnlyIn':'net.minecraftforge.api.distmarker.OnlyIn',
 'net.minecraftforge.fml.relauncher.Dist':'net.minecraftforge.api.distmarker.Dist',
 'appeng.api.AEApi':'appeng.core.Api',
 'appeng.tile.networking.ControllerTileEntity':'appeng.tile.networking.ControllerTileEntity',
 'appeng.parts.networking.PartDenseCable':'appeng.parts.networking.DenseCablePart',
 'appeng.parts.networking.PartCable':'appeng.parts.networking.CablePart'
};
const methods = {
 setTagCompound:'setTag', getTagCompound:'getTag', hasTagCompound:'hasTag', getCompoundTag:'getCompound',
 getTagList:'getList', getCompoundTagAt:'getCompound', tagCount:'size', getKeySet:'keySet', removeTag:'remove',
 hasKey:'contains', setInteger:'putInt', getInteger:'getInt', setLong:'putLong', setShort:'putShort', setByte:'putByte',
 setFloat:'putFloat', setDouble:'putDouble', setString:'putString', setBoolean:'putBoolean', setTag:'put',
 setUniqueId:'putUniqueId', getUniqueId:'getUniqueId', hasUniqueId:'hasUniqueId',
 getTotalWorldTime:'getGameTime', getFrontOffsetX:'getXOffset', getFrontOffsetY:'getYOffset',
 getFrontOffsetZ:'getZOffset', spawnEntity:'addEntity', addVector:'add',
 recieveMana:'receiveMana', canRecieveManaFromBursts:'canReceiveManaFromBursts'
};
for (const file of walk(path.join(root,'src/main/java')).filter(f=>f.endsWith('.java'))) {
 let s=fs.readFileSync(file,'utf8');
 for(const [a,b] of Object.entries(tokens)) s=s.replace(new RegExp('\\b'+a+'\\b','g'),b);
 for(const [a,b] of Object.entries(full)) s=s.split(a).join(b);
 for(const name of ['ICellInventory','ICellInventoryHandler','ICellHandler','ISaveProvider','ICellWorkbenchItem'])
   s=s.replaceAll('appeng.api.storage.'+name,'appeng.api.storage.cells.'+name);
 for(const [a,b] of Object.entries(methods)) s=s.replace(new RegExp('\\b'+a+'(?=\\s*\\()','g'),b);
 s=s.replace(/Platform\.openNbtData\(([^()]+)\)/g,'$1.getOrCreateTag()');
 s=s.replace(/net\.minecraft\.util\.text\.translation\.I18n/g,'net.minecraft.client.resources.I18n');
 s=s.replace(/I18n\.translateToLocalFormatted/g,'I18n.format').replace(/I18n\.translateToLocal/g,'I18n.format');
 s=s.replace(/\bposX\b/g,'getPosX()').replace(/\bposY\b/g,'getPosY()').replace(/\bposZ\b/g,'getPosZ()');
 s=s.replace(/\bisDead\b/g,'removed');
 s=s.replace(/appeng\.tile\.misc\.TileInterface/g,'appeng.tile.misc.InterfaceTileEntity');
 s=s.replace(/appeng\.fluids\.tile\.TileFluidInterface/g,'appeng.fluids.tile.FluidInterfaceTileEntity');
 if(file.includes(path.sep+'ae2'+path.sep)) {
  s=s.replaceAll('io.netty.buffer.ByteBuf','net.minecraft.network.PacketBuffer').replace(/\bByteBuf\b/g,'PacketBuffer').replace(/ throws IOException/g,'');
 }
 fs.writeFileSync(file,s,'utf8');
}
console.log('Migrated Java names and common APIs.');
