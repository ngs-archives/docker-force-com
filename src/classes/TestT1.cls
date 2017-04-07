@isTest public class TestT1{
  // Test for the trigger
  public static testMethod void test1(){
    Account a  = new Account(name='TEST');
    insert(a);
    a = [select id,description from Account where id=:a.id];
    System.assert(a.description.contains('t1_UPDATE2'));
  }
  // Test for the class
  public static testMethod void test2(){
    String s = C1.method1();
    System.assert(s=='HELLO2');
  }
}
