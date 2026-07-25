package dev.juaanp.barehanded;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
	public static final String MOD_ID = "barehanded";
	public static final String MOD_NAME = "Barehanded";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

	public static class Tags {
		public static final TagKey<Block> UNGRABBABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "ungrabbable"));
		public static final TagKey<Block> GRABBABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "grabbable"));
		public static final TagKey<Block> TREE_LOGS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "tree_logs"));
	}
}