# Drill in More Than 10 Minutes {-}

Part of the wonder of the modern internet is that one doesn't have to reinvent or _redescribe_ the wheel. The Apache Drill team has a superb "Drill in 10 Minutes"^[[Drill in 10 Minutes](https://drill.apache.org/docs/drill-in-10-minutes/)] tutorial which walks you through the downloading, installation and running (in "standalone" mode) of Apache Drill. _Provided you already have Java installed_, the time-investment estimation is fairly accurate.

You should go through that tutorial before proceeding and only continue here when Drill works. File an book issue if you believe more expository would help with that tutorial or if there were "gotchas" you had to overcoome.

## Beyond Standalone Mode

The "10 Minutes" tutorial puts Drill in standalone mode and gets you up and running. The `sergeant` package will work with Drill in standalone mode, but said mode requires you to keep a Drill "shell" running. That is sub-optimal and can lead to frustration. Thankfully, it's not too difficult to put drill into single-node server mode, but it does require a bit of work and one additional component: Apache Zookeeper^[[Apache Zookeeper](https://zookeeper.apache.org/)].

Zookeeper's mission in life it to be a highly available/distributed configuration manager for distributed services and when Drill is used in a >=1 configuration, Zookeeper is necessary to share configuration information between nodes. Thankfully, it's easy to setup Zookeeper in a single-node with Drill.

## Installing Zookeeper

First, we need to install Zookeeper:

### macOS

Using Homebrew^[[Homebrew](https://brew.sh/)]:

    brew install zookeeper
    ln -sfv /usr/local/opt/zookeeper/homebrew.mxcl.zookeeper.plist ~/Library/LaunchAgents

### Ubuntu/Debian-ish

    sudo apt-get install zookeeperd
    service zookeeper # {start|stop|status|restart|force-reload}

### Windows

   TBD

## Wiring Up Zookeeper With Drill

On Linux and macOS, it's best to have drill in `/usr/local/drill` (it's handy to symlink the full versioned directory to `drill` to make it easier to switch out/upgrade versions). On Windows, Drill is in `TBD`.

The Drill directory structure looks like:

    .
    ├── bin
    ├── conf
    ├── jars
    │   ├── 3rdparty
    │   │   ├── fedora
    │   │   ├── linux
    │   │   ├── osx
    │   │   └── windows
    │   ├── classb
    │   ├── ext
    │   ├── jdbc-driver
    │   └── tools
    ├── sample-data
    │   ├── nationsMF
    │   ├── nationsSF
    │   ├── regionsMF
    │   └── regionsSF
    └── winutils
        └── bin

And, you'll need to edit `conf/drill-override.conf` to look a bit like this:

    drill.exec: {
      cluster-id: "COOL_CLUSTER_ID_NAME",                                      # keep it short
      zk.connect: "localhost:2181",                                            # use zookeeper
      store.json.reader.skip_invalid_records: true,                            # handle bad json more gracefully
      sys.store.provider.local.path: "/usr/local/drill/conf/storage.conf"      # persist storage provider config changes
    }

Once you do that, you can then use:

    drillbit.sh start

to keep Drill running without having to persist a shell.

To test this configuration, just do:

    drill-conf

and you should get into a Drill `sqline` shell the same as if you had been using standalone mode.

The _cool_ part of this is that you can now extend your node into a cluster with just a few configuration changes.

## Allocating More Memory to Apache Drill

The author is a huge proponent of letting Drill do the data wrangling and giving it memory to do the work it needs to do for said wrangling vs R (or Python). For larger systems (16+DB RAM) you'll likely want/need to update the default memory settings.

To keep the book as DRY^[[Don't Repeat Yourself](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself)] as possible, you can check out the "Configuring Drill Memory" ^[[Configuring Drill Memory](https://drill.apache.org/docs/configuring-drill-memory/)] section of the online Drill manual since it does a fine job explaining the configuration parameters you'll need to change.

Drill is somewhat like R in that memory (RAM) will constrain what you can do. We'll discuss this more in future recipes.

## Administering Drill

The remainder of the book will assume Drill is running as a server. If you keep it in standalone mode, you will need to have it running manually for most of the book content to be relevant.

You'll want to bookmark/tab:

- <http://localhost:8047/>
- <http://localhost:8047/query>
- <http://localhost:8047/storage>
- <http://localhost:8047/profiles>

in your favourite web browser since it is likely you'll visit those admin pages more than you think you will. Starting with Drill 1.13.0 the query interface is syntax highlighted and a serious bug in the query results display has been fixed, so it's a pretty decent interface for testing out queries.

## Drill "Storage" Plugins

Drill works with databases, Hadoop-filesystems, Amazon S3 storage and plain 'ol local filesystems/directories that you use every day. We'll focus mainly on these simple local filesystems throughout the book since it is somewhat beyond the scope to introduce readers to Hadoop/Hive/HBase, but we'll likely delve into database access and even S3 access along the way.

You should Read The Fine Manual^[[Drill Storage Plugin Configuration](https://drill.apache.org/docs/storage-plugin-registration/)] to get an idea of how to work with the storage configuration since we'll be tweaking it along the way throughout the book.
