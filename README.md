2021-05-08 

in ns:
space.matterandvoid.dan-play

this fn:

(fqn-schema->str-keys reg immutable-reg)

gives you a schema that can be passed to the malli.dot/transform ns to generate a graphviz picture 
from your schema.

it is a hack by converting keyword keys to strings, but it works.


First you call that helper - then 
```clojure
(spit
    "malli-dot-schema.dot"
    (dot/transform Task))
```

then output a visual file:

```bash
dot -Tpdf malli-dot-schema.dot  -omalli-schema.pdf
```

todo: any inline function literals will fail you'll have to make a -simple-schema out of them.


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
