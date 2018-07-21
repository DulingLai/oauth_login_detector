# ALODE

Automatic detection and triggering of login screen in Android Applications

## Getting Started

### Prerequisites

This project depends on Soot (for modeling Android lifecycle) and IC3 (for decoding Intents), part of the code is borrowed from FlowDroid:

* [Soot](https://github.com/Sable/soot) : Soot - A Java optimization framework

* [IC3](http://siis.cse.psu.edu/ic3/) : Inter-Component Communication Analysis for Android

* [FlowDroid](https://github.com/secure-software-engineering/FlowDroid/) : FlowDroid Static Data Flow Tracker


### Setup

Please make sure that ic3-android.jar, AndroidCallbacks.txt and icc.cmodel are in the same directory as alode.jar

## Usage
```
java -jar {ALODE_JAR_PATH} alode [OPTIONS] [-cb <arg>] [-cg <arg>] [-d] [-h] -i <arg> [-l
         <arg>] [-o <arg>] [-s <arg>] [-t <arg>] [-v]
```

### Available Options
```
  usage: alode [OPTIONS] [-cb <arg>] [-cg <arg>] [-d] [-h] -i <arg> [-l
         <arg>] [-o <arg>] [-s <arg>] [-t <arg>] [-v]
   -cb <arg>           the maximum number of callbacks modeled for each
                       component (default to 20)
   -cg <arg>           callgraph algorithm to use (AUTO, CHA, VTA, RTA,
                       SPARK, GEOM); default: AUTO
   -d,--debug          debug mode (default disabled)
   -h,--help           print the help message
   -i,--input <arg>    input apk path (required)
   -l,--api <arg>      api level (default to 23)
   -o,--output <arg>   output directory (default to "sootOutput")
   -s,--sdk <arg>      path to android sdk (default value can be set in
                       config file)
   -t <arg>            maximum timeout during callback analysis in seconds
                       (default: 60)
   -v,--version        print version info
```

### Example Usages

To be updated later
```

## Authors

* **Duling Lai** - *Single Contributor* - [DulingLai](https://github.com/DulingLai)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

