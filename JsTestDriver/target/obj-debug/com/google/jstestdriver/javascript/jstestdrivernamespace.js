jstestdriver={};jstestdriver.global=this;jstestdriver.plugins={};jstestdriver.SERVER_URL="/query/";jstestdriver.HEARTBEAT_URL="/heartbeat";if(typeof console=="undefined"){console={}}if(typeof console.log=="undefined"){console.log=function(){}}if(typeof console.debug=="undefined"){console.debug=function(){}}if(typeof console.info=="undefined"){console.info=function(){}}if(typeof console.warn=="undefined"){console.warn=function(){}}if(typeof console.error=="undefined"){console.error=function(){}}jstestdriver.globalSetTimeout=setTimeout;jstestdriver.setTimeout=function(){if(jstestdriver.globalSetTimeout.apply){return jstestdriver.globalSetTimeout.apply(window,arguments)}return jstestdriver.globalSetTimeout(arguments[0],arguments[1])};jstestdriver.globalClearTimeout=clearTimeout;jstestdriver.clearTimeout=function(){if(jstestdriver.globalClearTimeout.apply){return jstestdriver.globalClearTimeout.apply(window,arguments)}return jstestdriver.globalClearTimeout(arguments[0])};jstestdriver.globalSetInterval=setInterval;jstestdriver.setInterval=function(){if(jstestdriver.globalSetInterval.apply){return jstestdriver.globalSetInterval.apply(window,arguments)}return jstestdriver.globalSetInterval(arguments[0],arguments[1])};jstestdriver.globalClearInterval=clearInterval;jstestdriver.clearInterval=function(){if(jstestdriver.globalClearInterval.apply){return jstestdriver.globalClearInterval.apply(window,arguments)}return jstestdriver.globalClearInterval(arguments[0])};document.write=function(a){throw new Error("Illegal call to document.write")};var noop=jstestdriver.EMPTY_FUNC=function(){};