WHOOOOOOOOOOOOOPPS
---
CUrrently not deploying due to some unresolved dependency, need to fix that...

Use
---
This assumes you have AEM installed locally and running at  http://localhost:4502/

* maven clean install will install the bundle
* use CRX to add some nodes, add mixin 'config:managed'
* add a properties file at the server using the name of the node you created or add config:name property to specify it
* optionally add config:additionalProfiles property to specify profiles (multi-value)
* add placeholder as: ${name.of.property:default value} where the default value is optional in any property of the added node
* save node and it should be picked up a little later and placeholder should be resolved.

TODO
---
* It currently only searches for nodes in /apps, this is consifugurable we should set a more sensible default
* Only listens to node add and remove events, this might miss some updates
* I think there are still issues with concurrency and order, I think placeholder updates and the Sling OsgiInstaller that picks 
 up the sling:OsgiConfig nodes that I'm typially using to add config:managed to, are running randomly, and installer might install a config before placeholder are resolved.
 We might need to fix this by hooking into the osginstaller of finding a way to get priority on events...
* Use the generic client implementation as it provides much of reusable boilerplate. 
* Dependencies are a mess, clean up!
* Consider a base system with some common osgi compat dependencies like base spring classes (even if just for the convenience of a RestTemplate e.g.)
  and some things like jackson etc. That can considerably clean up our bundles and give them all access to common useful libs
  