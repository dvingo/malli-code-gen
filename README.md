# Playing around

install gnu make > 4.0

then invoke:

    make

Or start a repl, choosing the aliases you want (see deps.edn):

```bash
clj -A:dev:test:custom-malli-registry:malli-instrument
```

# malli-code-gen
Define your schema, get free code.

later it would be nice to us bb tasks instead of make
install bb

https://github.com/babashka/babashka#installation

# JS 

Running the following starts a node.js build

    npm run dev

    node target/node-script.js

connect a shadow-cljs repl

nrepl remote use port in .shadow-cljs/.nrepl.port

(shadow/repl :main)

send node cljs forms

## Node.js tests

In a new shell process run:

    node target/node-tests.js
