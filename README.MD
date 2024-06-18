Projects
---
Collection of code snippets and examples to document my journey towards cleaner codes and elegant implementations.

## Introduction
This project also serves as a sandbox for exploring and experimenting with the various features that Spring offers. 
It allows the experience of setting up a new Spring project that might not typically be experienced when maintaining a brownfield project.

--- 
### BloomFilter 
A Distributed Redis BloomFilter inspired by Guava and Baqend.

#### Features
- Uses Redis pipelining to reduce RTT for improving performance
- Uses `BloomFilterManager` to manage created BloomFilter for easy reuse
- Usable but many improvements to be made...

---
### Criteria Engine
Designed to be able to create reusable business defined criteria(s) (stored in JSON objects) across multiple domains.
See `README` in the `projects-service/../projects/criteriaengine` folder for more details and usage examples

#### Features
- Business Criteria can be easily added/removed/changed
- Criteria can be reused easily once created
- Criteria conditions are clearly shown in codes
- Different strategies that can be used to configure the definition of success or failure of a criteria
- Processing might be slow due to the number of objects created, but for an application that is not latency sensitive, this should be alright

---
### Refreshable Cache 
Allow Redis keys to have a refresh duration where keys are not expired but asynchronously re-loaded in order
to prioritise availability over consistency.

#### Features
- Key reload is asynchronous 
- Reload retries upon next read if fails 