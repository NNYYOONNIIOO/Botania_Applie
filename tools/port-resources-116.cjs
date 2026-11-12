const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../src/main/resources'),assets=path.join(root,'assets/botania_applie');
const save=(p,data)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');};
const itemMap={
 'botania:manaresource:23':'botania:mana_powder','botania:manaresource:4':'botania:terrasteel_ingot',
 'botania:pool:0':'botania:mana_pool','botania:corporeaspark:0':'botania:corporea_spark',
 'botania:corporeaspark:1':'botania:corporea_spark_master','botania:auraring:0':'botania:aura_ring',
 'botania:auraringgreater:0':'botania:aura_ring_greater',
 'appliedenergistics2:part:460':'appliedenergistics2:me_p2p_tunnel',
 'appliedenergistics2:material:9':'appliedenergistics2:fluix_crystal',
 'appliedenergistics2:material:22':'appliedenergistics2:logic_processor',
 'appliedenergistics2:material:23':'appliedenergistics2:calculation_processor',
 'appliedenergistics2:material:28':'appliedenergistics2:crafting_card',
 'appliedenergistics2:material:39':'appliedenergistics2:storage_cell_housing'
};
function convert(value){
 if(Array.isArray(value))return value.map(convert);
 if(!value||typeof value!=='object')return value;
 if(value.type==='forge:ore_dict')return {tag:'forge:gems/certus_quartz'};
 if(value.item){let key=value.item+':'+(value.data||0);if(itemMap[key])value.item=itemMap[key];else if('data' in value)throw Error('Unknown legacy ingredient '+key);delete value.data;}
 return Object.fromEntries(Object.entries(value).map(([k,v])=>[k,convert(v)]));
}
const recipeDir=path.join(assets,'recipes');
for(const name of fs.readdirSync(recipeDir)){
 const file=path.join(recipeDir,name);let recipe=convert(JSON.parse(fs.readFileSync(file,'utf8')));
 if(name.includes('mana_storage_component_')||name.includes('mana_storage_cell_'))recipe.conditions=[{type:'botania_applie:config',option:'components'}];
 save(path.join(root,'data/botania_applie/recipes',name),recipe);fs.unlinkSync(file);
}
for(const name of ['en_us','zh_cn']){
 const file=path.join(assets,'lang',name+'.lang'),out={};
 for(const line of fs.readFileSync(file,'utf8').split(/\r?\n/)){if(!line||line.startsWith('#')||!line.includes('='))continue;const index=line.indexOf('=');let key=line.slice(0,index);if(key.startsWith('tile.'))key='block.'+key.slice(5);key=key.replace(/\.name$/,'').replace('botania_applie:','botania_applie.');out[key]=line.slice(index+1);}
 save(path.join(assets,'lang',name+'.json'),out);fs.unlinkSync(file);
}
for(const name of fs.readdirSync(path.join(assets,'models/item'))){
 const file=path.join(assets,'models/item',name),model=JSON.parse(fs.readFileSync(file,'utf8'));
 if(model.parent==='botania_applie:fluix_mana_pool')model.parent='botania_applie:block/fluix_mana_pool';
 if(model.loader)delete model.loader;
 save(file,model);
}
save(path.join(assets,'models/item/mana_packet.json'),{parent:'minecraft:item/generated',textures:{layer0:'botania_applie:items/mana_packet'}});
save(path.join(assets,'blockstates/fluix_mana_pool.json'),{variants:{'':{model:'botania_applie:block/fluix_mana_pool'}}});
save(path.join(root,'pack.mcmeta'),{pack:{description:'Botania Applie resources',pack_format:6}});
save(path.join(root,'data/botania_applie/loot_tables/blocks/fluix_mana_pool.json'),{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'botania_applie:fluix_mana_pool'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
for(const p of ['mcmod.info','botania_applie.interface.mixins.json','botania_applie.ae2fc.mixins.json','botania_applie.mekeng.mixins.json'])if(fs.existsSync(path.join(root,p)))fs.unlinkSync(path.join(root,p));
console.log('Migrated recipes, language keys, blockstate, loot and resource metadata.');
