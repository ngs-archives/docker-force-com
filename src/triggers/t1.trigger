trigger t1 on Account (before insert){
  for(Account a:Trigger.new){
    a.description = 't1_UPDATE';
  }
}

