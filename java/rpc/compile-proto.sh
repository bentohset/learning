 #!/bin/bash

 protoc -I=./ --java_out=./target ./src/main/proto/rpc.proto
 echo "Proto compiled"