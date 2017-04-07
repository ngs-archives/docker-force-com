docker-force-com
================


[![Docker Automated build](https://img.shields.io/docker/automated/atsnngs/radiko-recorder.svg?maxAge=2592000)](https://hub.docker.com/r/atsnngs/radiko-recorder/)

A Docker Image for CI/CD Salesforce Apex prorjects.

This project is still work in progress.

How to Use
----------

```sh
docker pull atsnngs/force-com

cat > envfile.txt <<ENVFILE
SF_USERNAME=you@example.com
SF_PASSWORD=PASSWORD+SECURITY_TOKEN
SF_SERVER=https://xxx-api.salesforce.com
ENVFILE

docker run \
  -v $(pwd)/wsdl:/wsdl \
  -v $(pwd)/src/classes:/src/classes \
  -v $(pwd)/src/triggers:/src/triggers \
  --rm \
  --env-file envfile.txt \
  atsnngs/force-com
```

Directory Structure
-------------------

```sh
.
├── src # Force IDE project sources
│   ├── classes
│   │   └── MyClass.cls
│   └── triggers
│       └── MyTrigger.trigger
│
└── wsdl # Download WSDL files, see refs
    ├── apex.wsdl
    └── enterprise.wsdl
```

TODOs
-----

- [ ] [Compile and test](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/sforce_api_calls_compileandtest.htm)

Refs
----

- [Downloading Salesforce WSDLs and Client Authentication Certificates](https://help.salesforce.com/HTViewHelpDoc?id=dev_wsdl.htm)
- [Force.com Web Service Connector (WSC)](https://github.com/forcedotcom/wsc)
