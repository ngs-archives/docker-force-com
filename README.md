docker-force-com
================

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
  -v $(pwd)/src:/src \
  --rm \
  --env-file envfile.txt \
  atsnngs/force-com
```

Directory Structure
-------------------

```
.
├── src
│   ├── classes
│   │   └── MyClass.cls
│   └── triggers
│       └── MyTrigger.trigger
└── wsdl
    ├── apex.wsdl
    └── enterprise.wsdl
```

TODOs
-----

- [ ] [Compile and test](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/sforce_api_calls_compileandtest.htm)

Refs
----

- https://github.com/forcedotcom/wsc
