
Hi, Spring fans! Welcome to another installment of Spring Tips! In this installment, we're going to look at the new features in Java 14 and their use in building Spring Boot-based applications. 


Speaker: [Josh Long (@starbuxman)](http://twitter.com/starbuxman)

<iframe width="560" height="315" src="https://www.youtube.com/embed/mr-7kGy8Yao" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>



To get started, we need to use the latest and greatest version of Java, Java 14, which isn't - just yet - released yet. It is due to be shipped in early 2020. You can download early access releases on [Java.net](https://jdk.java.net/). You might also consider using [SDKManager (`sdk`)](http://sdkman.io), which makes installing new JVM editions a trivial matter indeed. 

Remember, there are new Java releases every 6 months. These new releases are usable in production but are only supported for the six months between one release and the next. Every now and then, the Java project also releases a long-term support (LTS) release. That release is currently Java 11. Java 14 is only a viable target for production until Java 15 comes out. And indeed, we're going to look at a lot of _preview features_, which one might argue shouldn't be in production at all. You've been warned! 

If you're using SDKManager, you can run the following incantation to get Java 14 installed.

```shell 
sdk install java 14.ea.36-open 
```

Go to the [Spring Initializr](http://start.Spring.io) and generate a new project using Spring Boot 2.3 or later. You'll also need to select `JDBC` and `PostgreSQL`. 

Older versions of Spring Boot don't yet support the Java 14 runtime. Naturally, in order to edit this version of Java, you'll need to import it into your IDE. Before you do that, though, let's modify the `pom.xml` to configure the build to support Java 14. Normally, when you go to the Spring Initializr, you also specify a version of Java. Java 14 is not supported, yet, so we want to manually configure a few things. 

Make sure that you specify the version of Java by changing the `java.version` property:

```xml
<properties>
 <java.version>14</java.version>
</properties>
```

This allows our build to use Java 14 and all the released features in that release, but to really experience the novelty of Java 14, we need to turn on the _preview features_ - features that are shipped in the release but that are not active by default. 

In the `<plugins>...</plugins>` stanza, add the following plugin configurations to enable Java 14's preview features.  

```xml 
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>14</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
        <forceJavacCompilerUse>true</forceJavacCompilerUse>
        <parameters>true</parameters>
    </configuration>
</plugin>

<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

Now you're ready to go! Let's look at some Java code. The Spring Initializr was nice enough to give us a project and a skeletal entry point class: 

```java
package com.example.fourteen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;

@SpringBootApplication
public class FourteenApplication {

	public static void main(String[] args) {
		SpringApplication.run(FourteenApplication.class, args);
	}
}

```

We're going to create a simple JDBC-powered service that writes its data to the database using SQL. We'll need an object that maps to the data in the database table `people`: 

```sql 
create table people (
 id serial primary key ,
 name varchar(255) not null,
 emotional_state int not null 
);
```

At this point, I'd normally either slog through writing the Javabean object using my IDE's code-generation facilities, or I'd use Lombok to annotate my way to a compiler-synthesized object that has getters, setters, `toString`, and an implementation of `equals`. I might even make some begrudging reference to other languages' ability to make this tedious kind of work trivial. Scala supports _case classes_. Kotlin supports _data classes_. 

And Java 14 supports _record_s. 

```java
record Person(Integer id, String name, int emotionalState) {
}
```

Not bad eh? This syntax packs a wallop! It gives us a new object with a constructor and constructor parameters, properties, implementation of `equals` and `toString` and more. We can instantiate an instance of this object just as with any other object. Try to dereference properties in the object and you'll that our constructor properties have become `id()`/`id(int)`, `name()`/`name(String)`, and `emotionalState()`/`emotionalState(int)`. Not bad for so little! 



Let's look at the implementation of  `PeopleService`. 

The `PeopleService` uses the `JdbcTemplate` to make short work of turning results from a database query into Java objects. This should be fairly straightforward if you've ever used the `JdbcTemplate` (who hasn't)? I've left some parts unimplemented so we can revisit those directly. 

```java

@Service
class PeopleService {

	private final JdbcTemplate template;

	//todo
	private final String findByIdSql = null;

	private final String insertSql = null; 

	private final RowMapper<Person> personRowMapper =
		(rs, rowNum) -> new Person(rs.getInt("id"), rs.getString("name"), rs.getInt("emotional_state"));

	PeopleService(JdbcTemplate template) {
		this.template = template;
	}

	public Person create(String name, EmotionalState state) {
		 //todo
	}

	public Person findById(Integer id) {
		return this.template.queryForObject(this.findByIdSql, new Object[]{id}, this.personRowMapper);
	}
}


```


First and foremost, we're going to use some SQL queries. I've got to great pains in my life to avoid having to type SQL queries in Java code. My goodness, would people so often have used ORMs if they knew they could eloquently express SQL queries as Java ` Strings`? For anything even mildly complex, I extract my SQL queries into property files which are then loaded with Spring's configuration property mechanism.

But, we can do better in Java 14! Multiline strings have come to Java at long last! It now joins the ranks of Python, Ruby, C++, C#, Rust, PHP, Kotlin, Scala, Groovy, Go, JavaScript, Clojure, and a dozen other languages besides. I'm so happy it's finally here! 

Replace the `sql` variables with the following declarations. 

```java
private final String findByIdSql =
    """
            select * from PEOPLE 
            where ID = ? 
    """;

	private final String insertSql =
    """
        insert into PEOPLE(name, emotional_state)
        values (?,?);
    """;
```

So nice, that! There are methods you can use to trim the margin and so on. You can also use the backslash escape sequence (`\`) at the end of each line to signal that the next line should start there, otherwise the newlines are interpreted literally. 

Let's look at that `create` method. 

The storage of the `Person`'s `emotionalState` in the database as an `int` is an implementation detail. I'd prefer to not have to bubble that up to the user. Let's use an enum to describe the emotional state for each `Person`:

```java
enum EmotionalState {
	SAD, HAPPY, NEUTRAL
}
```

This is a start, I suppose. Let's get to the implementation. Straight away we're given an opportunity to use another nice new feature in Java 14: _smarter switch expressions_. Switch expressions give us a way to return a value from the branch of a switch case and then assign that to a variable. The syntax is _almost_ identical to what we've used before, except that each case is set off from the branch with an arrow, `->`, not `:`, and there's no need for a `break` statement. 

In the following example, we assign the `int` value to a variable `index`, whose type we don't need to specify because of yet another nice feature in recent Java iterations, auto type inference with `var`.

```java
	public Person create(String name, EmotionalState state) {
		var index = switch (state) {
			case SAD -> -1;
			case HAPPY -> 1;
			case NEUTRAL -> 0;
        };
        // todo 
	}
```

With the `index` in hand, we can create the requisite `PreparedStatement` required to execute the SQL statement against the database. We can execute that prepared statement and pass in a `KeyHolder` which will serve to collect the generated key returned from the newly inserted row. 


```java

	public Person create(String name, EmotionalState state) {
		var index = switch (state) {
			case SAD -> -1;
			case HAPPY -> 1;
			case NEUTRAL -> 0;
		};
        var declaredParameters = List.of(
            new SqlParameter(Types.VARCHAR, "name"), 
            new SqlParameter(Types.INTEGER, "emotional_state"));
		var pscf = new PreparedStatementCreatorFactory(this.insertSql, declaredParameters) {
			{
				setReturnGeneratedKeys(true);
				setGeneratedKeysColumnNames("id");
			}
		};
		var psc = pscf.newPreparedStatementCreator(List.of(name, index));
		var kh = new GeneratedKeyHolder();
		this.template.update(psc, kh);
		// todo
    }
```



The only trouble is that the key returned is a `Number`, not an `Integer` or a `Double` or anything more concrete. This gives us a chance to use yet another interesting new feature in Java 14, smart casting. Smart casting allows us to avoid a redundant cast after testing for a type in an `instanceof` test. It goes even further and gives us a variable name by which we can reference the automatically cast variable in the scope of the test. 

```java

	public Person create(String name, EmotionalState state) {
		var index = switch (state) {
			case SAD -> -1;
			case HAPPY -> 1;
			case NEUTRAL -> 0;
		};
        var declaredParameters = List.of(
            new SqlParameter(Types.VARCHAR, "name"), 
            new SqlParameter(Types.INTEGER, "emotional_state"));
		var pscf = new PreparedStatementCreatorFactory(this.insertSql, declaredParameters) {
			{
				setReturnGeneratedKeys(true);
				setGeneratedKeysColumnNames("id");
			}
		};
		var psc = pscf.newPreparedStatementCreator(List.of(name, index));
		var kh = new GeneratedKeyHolder();
		this.template.update(psc, kh);
		if (kh.getKey() instanceof Integer id) {
			return findById(id);
		}
		throw new IllegalArgumentException("we couldn't create the " + Person.class.getName() + "!");
    }
```

We needed an `int` to be able to pass it to `findById(Integer)`, and this method does that work for us. Convenient, eh? 


Everything's working, so let's exercise the code with a simple `ApplicationListener<ApplicationReadyEvent`:

```java

@Component
class Runner {

	private final PeopleService peopleService;

	Runner(PeopleService peopleService) {
		this.peopleService = peopleService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void exercise() throws Exception {
		var elizabeth = this.peopleService.create("Elizabeth", EmotionalState.SAD);
		System.out.println(elizabeth);
	}
}

```

Run that and you'll see that the object has been written to the database and - best of all - you got a spiffy new `toString()` result when printing the resulting `Person` object! 

We've only begun to scratch the surface of all the new features in Java 14! There are a ton of new features in the language that we've begun to introduce in this video and considerably more features for security and performance in the runtime itself. I can not more heartily recommend that you find a way off of your older versions of the JDK (looking at you, Java 8 users!) and move to the newest ones. 

