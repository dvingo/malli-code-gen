Given a malli schema generate lots of useful code and data structures; things like:

- pathom resolvers
- fulcro queries
- fulcro components
- the usual crud operations but with guardrails specs annotating them.
- generate sample data that is valid as per the data schema 


Designed to be as flexible as possible, I am thinking to achieve this you can 
use the interceptor pattern.

```clojure
(schema->code Task)
```
Takes malli schema as input and returns code as data (a list).
```clojure
(io.pedestal.interceptor.chain/execute {::schema MyEntityMalliSchema}
   (schema->resolver)
   (schema->fulcro-query)
   (schema->crux-pull-syntax))
```

This relies on the structure of your data.

The \*/id attribute of any entity you are defining schema for is special in that its value will be used as a pointer when
storing graph data in a normalized form.


I would like to support the use of idents as the pointer storage format, but use just a plain uuid for the id of each entity.
As far as I know the built in crux pull does not support this, this port of the datascript pull does:

https://github.com/dvingo/my-clj-utils/blob/master/src/main/dv/crux_pull.clj#L145

https://github.com/dvingo/my-clj-utils/blob/master/src/main/dv/crux_util.clj#L176

This should be made into a protocol or multi-method to allow open extension of how to map a pointer to an ID to an ID.
This would allow the helper to be customized.

When generating make-entity helpers, we do want to include nil values:

> storing explicit nil values in the source documents means the indexes can speed up certain queries,
> whereas if the attribute is simply missing then your query needs to use a rule-based approach (inevitably slower, and probably relying on full 
> index scanning), such as with this or-join https://github.com/juxt/crux/blob/06bc04139efabad5d0fe3dd779e76bd64bb42f46/crux-test/test/crux/query_test.clj#L1830-L1834

https://juxt-oss.zulipchat.com/#narrow/stream/194466-crux/topic/Querying.20for.20entities.20that.20may.20have.20a.20particular.20attribute/near/226542408

# Editors 
This strategy makes heavy use of generated symbols, and editors have issues resolving these thins.

If you use cursive you can disable unknown symbols via, resolve-as :none

https://github.com/cursive-ide/cursive/issues/2417
