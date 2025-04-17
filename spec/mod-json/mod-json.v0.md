```json5
{
"formatVersion": 0, // The Specification Revision
"name": "Example Mod", // The display name of the mod. (optional)
"id": "example-mod", // The identifier of the mod.
"description": "This is an example mod", // The description of the mod (optional)
"authors": [ "YOU :D" ], // The list of people who worked on or created the mod.
"meta": { // Extra meta-data of the example-mod (optional)
    "icon": "example-mod:icons/icon.png",
    "languageAdapters": {
      "groovy": "com.example.mod.providers.lang.GroovyLanguageAdapter"
    }
},
"entrypoints": { // The list of entrypoints that your mods will be launched from. (optional)
  "transformers": [ // Launches before the game starts.
    "com.example.mod.ExampleTransformerRegistrar"
  ],
  "preLaunch": [ // Launches right before the game starts, but after the transformers.
    "com.example.mod.ExamplePreLaunch"
  ],
},
"dependencies": { // 
    "puzzle-loader-core": ">=1.0.0"
},
"optional": {
    "example-dep1": "1.0.0...2.0.0",
    "example-dep2": ">=1.0.0",
    "example-dep3": ">1.0.0",
    "example-dep4": "1.0.0",
    "example-dep5": "*"
},
"mixins": [ // Mixins
    "example-mod.mixins.json",
    "example-mod.client-mixins.json"
],
"accessManipulator": "example-mod.manipulator", // Access Manipulator Path (optional)
"accessTransformer": "example-mod.cfg", // Access Transformer Path (optional)
"accessWidener": "example-mod.widener" // Access Widener Path (optional)
}
```