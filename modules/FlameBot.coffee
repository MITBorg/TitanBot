register = -> helper.register engine, 'onJoin', Java.type('mitb.event.events.JoinEvent')
getHelp = (event) -> helper.respond event, 'This module messages any user who joins with nickname *bot'

onJoin = (event) ->
  user = event.getSource().getUser()
  return if user? isnt true or user.getNick() is event.getSource().getBot().getNick()
  helper.respond event, 'im better than u noob bot' if user.getNick().toLowerCase().endsWith('bot')