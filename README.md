# openttd-robot : a goal robot with a set of game rules

openttd-robot work on the top of openttd-admin, it's purpose is to apply rules to the game.<br>
There is actually some rules built-in rules and of course it's extensible.

Usage instruction :

## 1.0 Setup  
### 1.1 Openttd  
open openttd.cfg and set an admin password :  
`admin_password = my_password`  
run a dedicated server :  
`openttd -D`  
run a client :  
`openttd -n 127.0.0.1`  
### 1.2 openttd-robot  
Launch the Hello World test  
`java -jar openttd-robot.jar com.openttd.robot.HelloWorldTest`
