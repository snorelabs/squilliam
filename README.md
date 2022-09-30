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
@DynamoDbBean
public class Employee {
    public static final String ITEM_TYPE = "EMPLOYEE";
    // ...
    @DynamoDbAttribute("EmployeeId")
    public String getEmployeeId() {
        return employeeId;
    }

    @ItemType(Employee.ITEM_TYPE)
    @DynamoDbAttribute("ItemType")
    public String getItemType() {
        return ITEM_TYPE;
    }
}

@DynamoDbBean
public class Manager {
    public static final String ITEM_TYPE = "MANAGER";
    // ...
    @DynamoDbAttribute("ManagerId")
    public String getManagerId() {
        return managerId;
    }

    @ItemType(Manager.ITEM_TYPE)
    @DynamoDbAttribute("ItemType")
    public String getItemType() {
        return ITEM_TYPE;
    }
    
    // Alternatively you could use Manager as the root and have employees on it!
    // @HasMany
    // private List<Employee> employees;
    // ^ These would be separately serialized and deserialized automagically
}

public class Team {
    @HasOne
    Manager manager;

    @HasMany
    List<Employee> employees;
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

Currently there is a rough V0 for model description, partition aggregation, and transformation.
Querying, hand off functionality to let you use anything native, and serializing all this data for
you is still to come. This will probably take another few weeks (ETA late October 2022). The harder
part is going to be intelligent relationship determination, Query building, and Immutable support.
