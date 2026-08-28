# Advanced Sound Addon for DynamX

AdvancedSoundAddon generates vehicle audio for DynamX in real time. It covers engines, turbochargers, tyres, brakes, horns, sirens, pneumatic systems and helicopter rotors without requiring vehicle sound samples.

## Preview

https://github.com/user-attachments/assets/46ba3333-d4c8-43f5-ac4d-60b40a131655

## Download

[![Latest release](https://img.shields.io/github/v/release/JarException/AdvancedSoundAddon?label=Latest%20release)](https://github.com/JarException/AdvancedSoundAddon/releases/latest)

## Installation

- Minecraft 1.12.2, Forge and DynamX are required.
- Install AdvancedSoundAddon on the client. Server installation is optional and enables synchronization for native custom horn and siren controls.
- BasicsAddon is optional.

Pack entries should use `#Op`, so the pack can still load when AdvancedSoundAddon is not installed:

```less
AdvancedSoundAddon#Op{
  Preset: I4_ROADSTER
}
```

## Engine sounds

Add the block to an `engine_NAME.dynx` file. Selecting a preset is normally enough; each preset already includes a suitable volume, RPM range, exhaust character and turbo behaviour.

| Family | Presets |
| --- | --- |
| Single / triple | `I1`, `I1_SCOOTER`, `I1_KART`, `I3`, `I3_ROAD`, `I3_CITY`, `I3_TURBO_ROAD`, `I3_SPORT`, `I3_BIKE` |
| Four / five cylinder | `I4`, `I4_LUXURY`, `I4_ROADSTER`, `I4_TURBO_SPORT`, `I4_BIKE`, `I5` |
| Inline six | `I6`, `I6_LUXURY_SPORT`, `I6_TURBO_SPORT`, `I6_PERFORMANCE` |
| V6 / flat six | `V6`, `V6_CLASSIC`, `V6_UTILITY_TURBO`, `V6_TWIN_TURBO`, `FLAT6`, `FLAT6_RACE` |
| Crossplane V8 | `V8_CROSSPLANE`, `V8_LUXURY_TURBO`, `V8_LUXURY_NA`, `V8_MUSCLE`, `V8_TRUCK`, `V8_SUPERCHARGED_CLASSIC`, `V8_SUPERCHARGED_MODERN`, `V8_SUPERCHARGED_SUV`, `V8_OFFROAD_RACE`, `V8_MARINE` |
| Flatplane / high-cylinder | `V8_FLATPLANE`, `V8_FLATPLANE_TURBO`, `V8_FLATPLANE_RACE`, `V10`, `V12`, `V12_LUXURY`, `V12_RACE`, `W16`, `W16_HYPERCAR` |
| Diesel | `I4_DIESEL`, `I4_DIESEL_REFINED`, `I4_DIESEL_UTILITY`, `I4_DIESEL_OFFROAD`, `I6_DIESEL`, `I6_BUS_DIESEL`, `I6_TRUCK_DIESEL`, `I6_HEAVY_DIESEL`, `V8_DIESEL`, `V8_DIESEL_ARMORED` |
| Alternative | `ELECTRIC`, `ELECTRIC_CITY`, `ELECTRIC_PERFORMANCE`, `ELECTRIC_UTILITY`, `TURBOSHAFT` |

Engines without a block use the neutral `I4` profile. Turbo sound is included automatically in matching presets; naturally aspirated, supercharged and electric presets do not receive a turbo.

### Optional engine tuning

Preset values can be overridden in the same block:

```less
AdvancedSoundAddon#Op{
  Preset: I6
  IdleRPM: 900
  AcousticMaxRPM: 6800
  OutputGain: 0.90
  ExhaustGain: 1.10
  IntakeGain: 0.38
  MechanicalGain: 0.20
}
```

Available overrides are `IdleRPM`, `AcousticMaxRPM`, `OutputGain`, `StarterRPM`, `StartDurationSeconds`, `StopDurationSeconds`, `ExhaustResonanceHz`, `IntakeResonanceHz`, `ExhaustGain`, `IntakeGain`, `MechanicalGain`, `PulseSharpness`, `InductionCharacter`, `MechanicalBrightness`, `PrimaryBankDelayMillis` and `SecondaryBankDelayMillis`.

Custom combustion layouts can additionally set `FiringOrder` and `FiringBanks`. `FiringOrder` must contain each cylinder number exactly once and supports 1–16 cylinders. `FiringBanks` contains one `0` or `1` for every firing event.

## Vehicle sounds

Vehicle features belong in `vehicle_NAME.dynx`. Several presets can share one block:

```less
AdvancedSoundAddon#Op{
  AirBrakePreset: TRUCK_AIR_BRAKE
  BrakeSquealPreset: CLASSIC_DISC
  HornPreset: TRUCK_AIR
}
```

| Feature | Presets | Default without property |
| --- | --- | --- |
| Helicopter rotor | `HELICOPTER_2_BLADE`, `HELICOPTER_4_BLADE` | Off |
| Air brakes | `TRUCK_AIR_BRAKE`, `BUS_AIR_BRAKE` | Off |
| Brake sound | `CLASSIC_DISC`, `CARBON_CERAMIC`, `OLD_DRUM` | Off |
| Exhaust afterfire | `SPORT`, `AGGRESSIVE`, `RACE` | Off |
| Tyres | `STREET_TIRE`, `PERFORMANCE_TIRE`, `RACE_SLICK`, `HEAVY_TIRE` | `STREET_TIRE` when globally enabled |
| Horns | `COMPACT_CAR`, `STANDARD_CAR`, `LUXURY_CAR`, `SPORT_CAR`, `CLASSIC_CAR`, `MOTORCYCLE`, `TRUCK_AIR`, `BUS_AIR`, `UTILITY`, `MARINE` | Original horn |

The corresponding properties are `RotorPreset`, `AirBrakePreset`, `BrakeSquealPreset`, `AfterfirePreset`, `TireSquealPreset` and `HornPreset`.

Use `TURBOSHAFT` together with a rotor preset for helicopters. Air brakes react when stopping and moving away, brake sounds play while braking, and afterfire is triggered by lifting off the throttle at high RPM.

A vehicle block may also select another engine `Preset` or override engine values. Vehicle values take priority over values from the engine file.

### Tyre sounds

Tyre sounds are disabled by default. Enable `enableTireSqueal` in `config/advancedsoundaddon.cfg` or Forge's mod config screen. While disabled, DynamX's original skid loop is muted as well. Once enabled, every wheeled vehicle uses `STREET_TIRE` unless another `TireSquealPreset` is selected.

### Horn controls

`HornPreset` enables the addon's horn for that vehicle. The default key is `K` and can be changed in the controls menu. If BasicsAddon is installed, its existing horn key is reused and no duplicate binding is registered. Vehicles without `HornPreset` keep their original horn.

When the server does not have AdvancedSoundAddon, native horn and siren controls remain available locally without adding networked vehicle modules. Signals supplied by BasicsAddon continue to use its existing synchronization.

### Sirens

Sirens are enabled per vehicle with `SirenPreset`:

```less
AdvancedSoundAddon#Op{
  SirenPreset: DE_FIRE
}
```

| Region | Presets |
| --- | --- |
| Germany | `DE_POLICE`, `DE_AMBULANCE`, `DE_FIRE` |
| France | `FR_POLICE`, `FR_GENDARMERIE`, `FR_FIRE`, `FR_SAMU`, `FR_AMBULANCE` |
| United States | `US_WAIL`, `US_YELP`, `US_HI_LO`, `US_PRIORITY`, `US_RUMBLER_WAIL`, `US_RUMBLER_YELP`, `US_Q_SIREN` |
| Generic Europe | `EU_HI_LO` |

The default toggle key is `I`. If BasicsAddon already controls the vehicle's siren, AdvancedSoundAddon follows that state and reuses its key binding. Vehicles without `SirenPreset` keep their original siren.

## Custom horns and sirens

Built-in presets are recommended. `CUSTOM` is available when no preset fits the vehicle.

```less
AdvancedSoundAddon#Op{
  HornPreset: CUSTOM
  HornSource: ELECTRIC_TRUMPET
  HornFrequenciesHz: 390 470
  HornRelativeGains: 1.0 0.72
  HornAttackSeconds: 0.015
  HornHoldSeconds: 0.45
  HornReleaseSeconds: 0.14
  HornGain: 0.90
  HornBrightness: 0.55
  HornRasp: 0.20
  HornAudibleDistance: 60
}
```

`HornSource` accepts `ELECTRIC_DISC`, `ELECTRIC_TRUMPET`, `AIR_TRUMPET` or `MARINE_TRUMPET`.

```less
AdvancedSoundAddon#Op{
  SirenPreset: CUSTOM
  SirenSource: AIR_HORN
  SirenPattern: STEP
  SirenFrequenciesHz: 430 690
  SirenSecondaryFrequenciesHz: 445 710
  SirenDurationsSeconds: 0.55 0.55
  SirenHarmonics: 1.0 0.35 0.15 0.06
  SirenGain: 1.0
  SirenRasp: 0.25
  SirenFlutterHz: 6
  SirenFlutterDepth: 0.004
  SirenSubharmonicGain: 0.0
  SirenAudibleDistance: 105
}
```

`SirenSource` accepts `AIR_HORN`, `ELECTRONIC_SPEAKER` or `MECHANICAL_ROTOR`. `SirenPattern` accepts `STEP`, `TRIANGLE`, `SINE`, `SAW_UP` or `SAW_DOWN`; `AIR_HORN` requires `STEP`.

## Building

Use JDK 17 and run `./gradlew clean build`. The output is written to `build/libs/`.
Increase `mod_version` and the `VERSION` constant together before publishing a new release.

## License

AdvancedSoundAddon is source available under its [custom license](LICENSE). Forking, private modifications and pull requests are welcome. Publishing or redistributing independent builds, releases or modified versions requires written permission from JarException. See [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes.

## Third-party licenses and credits

Third-party license and attribution details are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

German siren presets were calibrated using an openly licensed [Essen police-siren recording](https://www.soundsofchanges.eu/sound/konrad-gutkowski-57/) and published [MARTIN-HORN 2298](https://www.maxbmartin.de/produkte/martin-horn-2298-gm) specifications. No siren recordings are included in the addon.

[DynamX website](https://dynamx.fr) · [DynamX wiki](https://dynamx.fr/wiki/)
