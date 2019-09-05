#TODO: Refazer script

#Remove DB
rm homeserver_test.db

#Start Synapse
source env/bin/activate

./synctl restart homeserver_test.yaml

#Run Tests
./e2e_tests/01.criar_usuario_com_empresa.sh

#Deactivate env
deactivate

