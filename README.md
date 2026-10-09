# GT Coil Info for EMI

A client-side Minecraft Forge 1.20.1 mod that shows **which GregTech heating coil
a recipe needs**, drawn directly onto GregTech's own EMI recipe pages.

GregTech tells you a recipe's temperature, but not which coil clears it — and for
custom multiblocks registered through KubeJS it shows nothing at all. This mod
fills that gap.

## How it works

* An `EmiRecipeDecorator` adds one extra slot to every matching GregTech recipe.
* The coil list is read from the **block registry at runtime**, so coils added by
  KubeJS (`event.create('x_coil_block', 'gtceu:coil')`) show up automatically.
* The recipe's requirement is read from the recipe's own data tag — the value you
  set with `.addData("RequiredTemp", 1000)` in a KubeJS recipe.
* GregTech is accessed purely by reflection, so the build has no GregTech,
  LDLib or Registrate dependency.

## Configuration

`config/gtcoilemi.json` is written on first launch.

```json
{
  "enabled": true,
  "namespaces": ["gtceu", "kubejs"],
  "autoDetect": true,
  "autoDetectKeys": ["ebf_temp", "RequiredTemp", "required_temp", "coil_temp", "temperature"],
  "categories": {
    "gtceu:electric_blast_furnace": { "dataKey": "ebf_temp", "mode": "temperature" }
  },
  "offsetX": 0,
  "offsetY": 0
}
```

To wire up your own multiblock, add its recipe category id and the data key your
recipes use. `mode` picks which coil property the number is compared against:

| mode          | matched against          | used by                        |
|---------------|--------------------------|--------------------------------|
| `temperature` | coil temperature in K    | Electric Blast Furnace and co. |
| `level`       | coil level               | Multi Smelter parallels        |
| `tier`        | coil tier                | Pyrolyse Oven, Cracking Unit   |

If a category has no entry, `autoDetectKeys` is tried in order.

## Building

See [BUILDING.md](BUILDING.md).
