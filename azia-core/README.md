Azia Core
---------

A generic transaction framework for multi-threaded desktop
applications, this pure Java module serves as the foundation
of the [Azia User Interface Library][parent].

[parent]: https://github.com/byron-hawkins/org.hawkinssoftware.azia/blob/master/azia/README.md

#### Artifact

A pure Java library.

#### Installation

Add it as a project dependency, like any ordinary Java library.

#### Usage

1. A transactions is initiated by submitting a subclass of 
   the abstract <code>[UserInterfaceTask]</code> to 
   <code>[TransactionRegistry].executeTask()</code>, which will 
   use only the calling thread.
    * Obtain a transaction by calling `this.getTransaction()`
      from within the task's `execute()` method.
        + the transaction will be constructed if necessary
        + if so, it automatically joins any other transactions 
          open on the task thread
    * If a [RetryException][UserInterfaceTask] is caught within 
      the `execute()` implementation, it must be rethrown 
    * Return `true` to approve transaction commit
    * Return `false` to request transaction rollback
1. Mutable fields of all classes must be annotated with
   [@ValidateRead] and [@ValidateWrite] to maintain data integrity
    * Every validated class must be relatable to an instance
      of [UserInterfaceActor]
       + An entity not wishing to be an actor itself may instead 
         choose to be a [UserInterfaceActorDelegate]
       + An entity having no direct reference to any actor may
         look up the most relevant instance using 
         <code>[CompositionRegistry].getService(UserInterfaceActor.class)</code>
    * Mutable fields may only be written during the commit phase
      a transaction; see [azia-ui] about [UserInterfaceDirective]

[azia-ui]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-ui/blob/master/azia-ui/README.md
[CompositionRegistry]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-ui/blob/master/azia-ui/src/main/java/org/hawkinssoftware/azia/ui/component/composition/CompositionRegistry.java
[TransactionRegistry]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-core/blob/master/azia-core/src/main/java/org/hawkinssoftware/azia/core/action/TransactionRegistry.java
[UserInterfaceActor]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-core/blob/master/azia-core/src/main/java/org/hawkinssoftware/azia/core/action/UserInterfaceActor.java
[UserInterfaceActorDelegate]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-core/blob/master/azia-core/src/main/java/org/hawkinssoftware/azia/core/action/UserInterfaceActorDelegate.java
[UserInterfaceDirective]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-core/blob/master/azia-core/src/main/java/org/hawkinssoftware/azia/core/action/UserInterfaceDirective.java
[UserInterfaceTask]: https://github.com/byron-hawkins/org.hawkinssoftware.azia-core/blob/master/azia-core/src/main/java/org/hawkinssoftware/azia/core/action/UserInterfaceTask.java
[@ValidateRead]: https://github.com/byron-hawkins/org.hawkinssoftware.rns-core/blob/master/rns-core/src/main/java/org/hawkinssoftware/rns/core/validation/ValidateRead.java
[@ValidateWrite]: https://github.com/byron-hawkins/org.hawkinssoftware.rns-core/blob/master/rns-core/src/main/java/org/hawkinssoftware/rns/core/validation/ValidateWrite.java
