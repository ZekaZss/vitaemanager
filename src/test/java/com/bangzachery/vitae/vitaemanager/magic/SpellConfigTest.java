package com.bangzachery.vitae.vitaemanager.magic;
import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import static org.junit.jupiter.api.Assertions.*;
class SpellConfigTest {
 @Test void unsafeMovementSpeedAndNonfiniteValuesAreRejectedBeforeUse(){var c=new YamlConfiguration();for(double value:new double[]{-1,0,Double.NaN,Double.POSITIVE_INFINITY,1}){c.set("rise-per-tick",value);assertThrows(IllegalArgumentException.class,()->SpellConfig.range(c,"rise-per-tick",.05,.001,.25));}c.set("rise-per-tick",.05);assertDoesNotThrow(()->SpellConfig.range(c,"rise-per-tick",.05,.001,.25));}
 @Test void customSoundNamesAllowResourcePackIdsAndRejectCommandsOrInvalidKeys(){assertDoesNotThrow(()->SpellConfig.validateSoundSyntax("custom","itemsadder:magic.pressure"));assertDoesNotThrow(()->SpellConfig.validateSoundSyntax("vanilla","ENTITY_BAT_AMBIENT"));for(String invalid:new String[]{"/playsound test","pack:bad key","UPPER:invalid","x:y:z",""})assertThrows(IllegalArgumentException.class,()->SpellConfig.validateSoundSyntax("custom",invalid));assertThrows(IllegalArgumentException.class,()->SpellConfig.validateSoundSyntax("command","minecraft:ambient.cave"));}
 @Test void healingAmplifierAndIntervalsAreBounded(){var c=new YamlConfiguration();c.set("healing.level",128);assertThrows(IllegalArgumentException.class,()->SpellConfig.range(c,"healing.level",10,1,20));c.set("interval",0);assertThrows(IllegalArgumentException.class,()->SpellConfig.range(c,"interval",50,1,72000));}
}
