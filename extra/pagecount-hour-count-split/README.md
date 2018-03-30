# Simple Mask Function for Apache Drill

This project contains a simple example for a custom function for Apache Drill.

This functions is a mask one that allows the following queries to be executed:

```
SELECT MASK(first_name, '*' , 3) FIRST , MASK(last_name, '#', 7) LAST  FROM cp.`employee.json` LIMIT 5;
+----------+------------+
|  FIRST   |    LAST    |
+----------+------------+
| ***ri    | ######     |
| ***rick  | #######    |
| ***hael  | ######     |
| ***a     | #######ez  |
| ***erta  | #######    |
+----------+------------+
```

Look at [this blog post](http://tgrall.github.io/blog/2015/07/20/apache-drill-how-to-create-a-new-function/) for the step by step tutorial.


## How to Compile Install

Clone and compile.

```

git clone https://github.com/tgrall/drill-simple-mask-function.git

cd drill-simple-mask-function

mvn package

```

Now download and unpack Apache Drill.

```
wget http://getdrill.org/drill/download/apache-drill-1.1.0.tar.gz
tar xvf apache-drill-1.1.0.tar.gz
```

Copy the jar files from your functions into the 3rdparty directory in the Drill distro

```
cp drill-simple-mask-function/target/*.jar apache-drill-1.1.0/jars/3rdparty
```

Now run drill and test the results

```
$ cd apache-drill-1.1.0/
$ bin/drill-embedded
0: jdbc:drill:zk=local>
SELECT MASK(first_name, '*' , 3) FIRST , MASK(last_name, '#', 7) LAST  FROM cp.`employee.json` LIMIT 5;
+----------+------------+
|  FIRST   |    LAST    |
+----------+------------+
| ***ri    | ######     |
| ***rick  | #######    |
| ***hael  | ######     |
| ***a     | #######ez  |
| ***erta  | #######    |
+----------+------------+
```


