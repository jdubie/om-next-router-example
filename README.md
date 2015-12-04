# Om Next Router

A simple om next router that demonstrates client-side url-based routing,
browser history.

The `Root` component listens for browser history events and syncs those to it's
query and om state accordingly.

The `Root` component listens for om state changes and syncs those to it's query
and browser history.

The `Root` component providing bi-directional syncing between om state and
browser url is nice because now your parsing just deals with pure data and no
browser / DOM manipulation.

```
git clone https://github.com/jdubie/om-next-router
make
```
