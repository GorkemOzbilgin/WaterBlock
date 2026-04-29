# WaterBlock
A [ProjectKorra](https://github.com/ProjectKorra/ProjectKorra) addon ability.

With this new version, you can now add other abilities to blocking list, including your own custom abilities.  
In addition to this, now you can edit WaterBlock's combination, description and instructions.  
  
In the config file;  
Add your ability to the section named `BlockAbilities` with the format `AbilityName:WaveRange:BlockLimit`  

For more information, you can it on check here: [ProjectKorra Forums](https://projectkorra.com/forum/resources/waterblock-by-dreamerboy-and-hiro.396/)
  
Example Config File:  
```yml
ExtraAbilities:
  DreamerBoy:
    Water:
      WaterBlock:
        Cooldown:
          Cooldown: 8000
          CooldownOnOutOfTheSightView: true
        Duration: 0
        Knockback: 2
        Radius: 2
        BlockAbilities:
        - Torrent:8:1
        - Surge:8:1
        - WaterManipulation:4:3
        - Geyser:8:2 #This will make your WaterBlock to block Geyser ability 2 times in a row. Also its wave range will be 8 blocks.
        Combination:
        - PhaseChange:SNEAK_DOWN
        - PhaseChange:LEFT_CLICK
```
