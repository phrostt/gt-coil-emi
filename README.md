# GT Coil Info for EMI

A client-side Minecraft Forge 1.20.1 mod that shows **which GregTech heating coil
a recipe needs, and whether it's gated behind research**, drawn directly onto
GregTech's own EMI recipe pages.

GregTech only shows coil/temperature requirements natively for the Electric Blast
Furnace, Cracker, and Pyrolyse Oven — custom multiblocks registered through KubeJS
show nothing at all, and research requirements aren't shown anywhere outside the
Assembly Line's own UI. This mod fills both gaps.

## How it works

* An `EmiRecipeDecorator` adds extra slots to every matching GregTech recipe.
* **Coil requirement:** the coil list is read from the **block registry at
  runtime**, so coils added by KubeJS (`event.create('x_coil_block', 'gtceu:coil')`)
  show up automatically. The recipe's requirement is read from the recipe's own
  data tag — the value you set with `.addData("RequiredTemp", 1000)` in a KubeJS
  recipe.
* **Research requirement:** if a recipe carries a GregTech `ResearchCondition`
  (added via `.scannerResearch(...)` or `.stationResearch(...)`), the data item it
  demands (data stick, orb, or module) is drawn as a badge in the same spot as the
  coil icon. Assembly Line recipes are skipped since GregTech already shows this
  natively via the Data Access Hatch slot.
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
  "showResearchIcon": true,
  "researchExcludedCategories": ["gtceu:assembly_line"],
  "offsetX": 0,
  "offsetY": 0
}
```

To wire up your own multiblock, add its recipe category id and the data key your
recipes use. `mode` picks which coil property the number is compared against:

| mode          | matched against          | used by                        |
|---------------|--------------------------|---------------------------------|
| `temperature` | coil temperature in K    | Electric Blast Furnace and co. |
| `level`       | coil level               | Multi Smelter parallels        |
| `tier`        | coil tier                | Pyrolyse Oven, Cracking Unit   |

If a category has no entry, `autoDetectKeys` is tried in order.

The research badge is controlled separately: `showResearchIcon` is a master
toggle, and `researchExcludedCategories` lists categories to skip (Assembly Line
by default - add more here if another machine ever shows its own research slot
natively).

## Building

See [BUILDING.md](BUILDING.md).
