package com.CSC584.villagegrowth.mixin;

import net.minecraft.structure.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.class)
public interface StructureTemplateInterfaceMixin {
    @Accessor
    List<StructureTemplate.PalettedBlockInfoList> getBlockInfoLists();
}
