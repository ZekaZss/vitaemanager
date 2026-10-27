package com.bangzachery.vitae.vitaemanager.magic;
import org.bukkit.entity.Player;
/** A new mechanic is Java code with explicit lifecycle; YAML only supplies parameters. */
interface SpellMechanic {
 String id();
 default void validate(Player owner,SpellConfig config){}
 default void begin(MagicService service,MagicService.Use use,Player owner){}
 default void cleanup(MagicService service,MagicService.Use use){}
 void tick(MagicService service,MagicService.Use use)throws Exception;
}
