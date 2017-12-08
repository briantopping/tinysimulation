# TinySimulation

This is another go at the project requirements provided by Alejandro. I took the liberty to restart the project after a rather surprised email from Avinash regarding the previous effort. 

This effort is a much more theoretical one put together from first principles with minimal dependencies. 

To run: Install SBT on your machine (i.e. with `brew install sbt`) then`sbt run`.

## Questions to Ponder

* **Q**: What strategy would you use to load-balance POST requests across service nodes? <br/>
**A**: As in the previous version, I ran out of time (too close to phone call rather than hitting 4 hour mark), but my intent was to keep a scoreboard `Map` in the web server that was updated by periodic heartbeats from backend nodes. The heartbeat responses would contain the current number of entires for a backend so the frontend could decide which node to send new POSTs to.

* **Q**: What strategy would you use to load-balance GET requests? <br/>
**A**: This one is easier, because the node ID is encoded in the shortened URL, dispatching is simple.

## Notes
* *Friday, December 8, 2017 at 3:01:56 PM* - Added this readme and the `sbt run` command to the current code, which I previously clocked at 2.4 hours.
* *Friday, December 8, 2017 at 4:14:34 PM* - Cleaned up URL handling to spec, added actual redirect functionality at `http://127.0.0.1/urlshortener/<key>` that actually works in a browser :) At 3.4 hours.