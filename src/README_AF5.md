# Axon Framework 5 concerns:

1. `ProcessingContext` how we will achieve that? Are we going to resolve it in handlers?
   I'm not sure if this should be user responsibility, or the framework (with normal usage, without any customizations).
2. `commandGateway.sendAndWait` - there is no version with ProcessingContext
3. Returning Void from Command send requires something like
   ` commandGateway.send(command, GameMetaData.withId(gameId), processingContext).resultAs(Void.class)`
4. `RecruitCreatureCommandHandler` - ok, I have events from 3 (streams?). Thinking in DCB influences the model a lot!
   Assume that Dwelling and Armies are totally decoupled (till!) i `RecruitCreature` and I need to know if I can (maybe
   my army already has max creatures). Then should I produce just one event with tags or 3 events?
5. It's complex to play around in CommandHandler with completable future. 
   I will be in favor of providing State as a parameter (use repository under the hood) and just return events (publish by the framework)