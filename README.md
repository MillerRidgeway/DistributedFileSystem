Info
====

Distributed file system that utilizes a central controller node for organization and host nodes for redundancy and serving. Written entirely in Java with no library support *other* than for erasure encoding (Reed-Solomon).


Usage
=================

**Controller**: HOST_PORT | ENCODING

**Client**: CONTROLLER_ADDR | CONTROLLER_PORT | ENCODING | TEMP_DIR

**Chunk server**: CONTROLLER_ADDR | CONTROLLER_PORT | STORAGE_DIR

**ENCODING:** If set to "erasure", uses Reed-Solomon to encode chunks. 
Anything else replicates the shards across multiple servers.

**TEMP_DIR:** A directory to store intermediary and output files. Intermediary files will be removed by the program before the user can see them. 
