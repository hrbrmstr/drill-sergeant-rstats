# Adding a New 'Workspace' to Drill

## Problem

You have data in a directory structure and would like a Drill "shortcut" reference to it vs entering the full path in queries all the time.

## Solution

Create a Drill "`workspace`".

## Discussion

If you've gone through [Drill in 10 Minutes](https://drill.apache.org/docs/drill-in-10-minutes/) or reviewed the recipe that goes into a bit more depth on an introduction to Drill, you know you can get to any location on your local filesystem with `dfs.root` filesystem references like this:

    dfs.root.`/some/very/long/path/to/a/set/of/files/in/my/coolproject/*.json.gz`

That's great but it's also annoying to type each time you work with data in that directory.

Drill lets you define a [workspace](https://drill.apache.org/docs/workspaces/) name as a kind of alias to a filesystem location. They're very easy to setup by going to <http://localhost:8047/storage/dfs> and taking a look at the JSON configuration under the `dfs` storage plugin. There's an entry for "`workspaces`" and we can add one for the above example like so:

    "coolproject" : {
      "location" : "/some/very/long/path/to/a/set/of/files/in/my/coolproject/",
      "writable" : false,
      "defaultInputFormat": null
    },

Now, you can use:

    dfs.coolproject.`/*.json.gz` 

in queries.

If you have custom formats or just know the file format most files will be using in your directory tree, you can also customize the `defaultInputFormat` and if you _really_ want to live dangerously you can make your directory tree writable by changing that boolean value to `true`. Drill is pretty good about not overwriting files and directories but unless you really need write-ability, leave this `false`.

## See Also

- [Connect a Data Source](https://drill.apache.org/docs/connect-a-data-source-introduction/)
