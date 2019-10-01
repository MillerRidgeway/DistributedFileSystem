tab="--tab-with-profile=Default"
foo=""
cmd="bash -c 'java Controller.Controller 51000 erasure'"


mkdir ~/ChunkServers/Chunk{1..8}

cd ~/Projects/DistributedFileSystem/src/
foo+=($tab -e "$cmd")   
gnome-terminal "${foo[@]}"

i=8
for i in {1..8}
do
	cmd="bash -c 'java ChunkServer.ChunkServer 127.0.0.1 51000 ~/ChunkServers/Chunk${i}/'"
	foo=($tab -e "$cmd")
	gnome-terminal "${foo[@]}"
done

cmd="bash -c ' java -cp ./*:.:../lib/reed-solomon-erasure-coding.jar Client.Client 127.0.0.1 51000 erasure ~/Projects/DistributedFileSystem/'"
foo=($tab -e "$cmd")
gnome-terminal "${foo[@]}"

exit 0

