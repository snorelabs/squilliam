# Squilliam

Squilliam currently is a bunch of garbage that's doing nothing. But let's imagine for a second that
this project made your life as a Java engineer using DynamoDB _almost_ easy. That's right, this is
what I'm selling ya on.


## DynamoDB and Heterogeneous Types

A key feature of Dynamo is Single Table Design. When you're working with multiple types of data
this often involves lumping together various object types into one table. Sometimes you want a couple
of different types to be returned!

Have you ever been a victim of having to write something like this?

```java
public class TeamRepository {
    public Team query(QueryRequest request) {
        List<Map<String, AttributeValue>> items = client.query(query).items();
        Team root = new Team();
        List<Employee> employees = items.stream()
                .filter(item -> item.get("Type").s().equals("Employee"));
        Manager manager = items.stream()
                .filter(item -> item.get("Type").s().equals("Manager")).get(0);
        root.setEmployees(employees);
        root.setManager(manager);
        return root;
    }
}
```

If so, I'm sorry for your loss of time, effort, sanity, comfort, uhhhh whatever else might have
wronged you about this particular convention. I would like to fix it!

## Using Squilliam

So, how do we make this any better? Well, annotations which describe the relationships your data
might have _after_ it comes back from DynamoDb!

```java
@ItemType("Employee")
@DynamoDbBean
public class Employee {
    // ...
}

@ItemType("Manager")
@DynamoDbBean
public class Manager {
    // ...
    
    // Additionally, your Manager class could have many employees
    // and you could use it as a root!
    
    // @HasMany
    // Array<Employee> employees;
}

public class Team {
    @HasOne
    Manager manager;

    @HasMany
    Array<Employee> employees;
}

public class TeamService {
    public Team getTeam() {
        // Currently, you define what you'd like to query on (but not for long ;)
        QueryRequest query;
        
        // AND THEN MAGIC!!!
        Team team = squilliam.load(query, Team.class);
    }
}
```

## Unfortunately, It's Not Ready

Currently, the partition transformation process has a rough V0. Building the Target Model information
and querying, hand off functionality to let you use anything native, and serializing all this data for
you is still to come. This will probably take another few weeks (ETA late October 2022). The harder
part is going to be intelligent relationship determination, Query building, and Immutable support.
