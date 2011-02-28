package org.gcontracts.tests.doc

import org.gcontracts.tests.basic.BaseTestClass

/**
 * @author ast
 */
class RootClassExampleTests extends BaseTestClass {
  
  def source = ''' 
package tests

import org.gcontracts.annotations.*

@Invariant({ field1 > 0 })                            
class RootClass {                                     
    
  // made field protected due to groovy compilation bug
  protected Integer field1              
  private Integer field2
  private Integer field3                                                                 
                                  
  private Date dateField1                   
                                                         
  Integer property1                                                            
                                                                   
  RootClass(final Integer attribute)  {                
    field1 = attribute                                                           
  }                                                  

  @Requires({ paramAttribute1 > 1 && paramAttribute2 > 1 })
  def void some_operation(final Integer paramAttribute1, final Integer paramAttribute2)  {
    this.field1 = paramAttribute1
    this.field2 = paramAttribute2
  }                                                                                                            
                                                   
  @Ensures({ field1 == paramAttribute1 })                     
  def void some_operation2(final Integer paramAttribute1)  {           
    field1 = paramAttribute1
  }                                   

  @Ensures({ old -> old.field1 != paramAttribute1 })            
  def void some_operation3(final Integer paramAttribute1)  {
    field1 = paramAttribute1
  }

  @Ensures({ result -> result == paramAttribute1 + paramAttribute2 })
  def int some_operation4(final Integer paramAttribute1, final Integer paramAttribute2)  {
    return paramAttribute1 + paramAttribute2
  }               
                                                     
  @Ensures({ result -> result == field3 })
  def int some_operation5(final Integer paramAttribute1, final Integer paramAttribute2)  {
    field3 = paramAttribute1 + paramAttribute2
    return field3
  }
                                                                                                            
  @Ensures({ old, result -> old.field1 != field1 && old.field2 != field2 && field3 == result })
  def some_operation6(def param1, def param2)  {
    field1 = param1
    field2 = param2                                                       
    field3 = param1 + param2                                                        
    return field3
  }                                                                        
                                                                                                                 
  @Ensures({ result, old -> old.field1 != field1 && old.field2 != field2 && field3 == result })
  def some_operation7(def param1, def param2)  {
    field1 = param1
    field2 = param2
    field3 = param1 + param2                                                                                                       
    return field3
  }                             

  @Ensures({ result -> result == param1 })                           
  def some_operation8(def param1)  {                                                                                            
    param1
  }

  @Ensures({ result -> result == param1 + param2})
  def some_operation9(def param1, def param2)  {
    param1 + param2
  }                                                  

  @Ensures({ old -> old.dateField1 != param1 && dateField1 == param1 })
  def void some_operation10(def param1)  {
    dateField1 = param1
  }

  @Requires({ param1 > 10 })
  def void some_operation11(def param1)  {
    field1 = param1
  }
                           
  @Ensures({ old -> old.dateField1 != param1 && dateField1 == param1 })
  def void some_operation12(def param1)  {
    dateField1 = param1
  }
}
'''
  
  def void test_class_invariant()  {
    create_instance_of(source, [1])
  }                             

  def void test_class_invariant_fail()  {

    shouldFail AssertionError, {
      create_instance_of(source, [0])
    }
  }                                                   

  def void test_class_invariant_with_default_constructor()  {
    shouldFail AssertionError, { create_instance_of(source) }
  }

  def void test_precondition_with_multiple_arguments()  {

    def root = create_instance_of(source, [1])

    root.some_operation 2, 2
  }

  def void test_precond_with_first_argument_fail()  {

    def root = create_instance_of(source, [1])

    shouldFail AssertionError, {
      root.some_operation(1, 2)
    }
  }

  def void test_precond_with_second_argument_fail()  {

    def root = create_instance_of(source, [1])

    shouldFail AssertionError, {
      root.some_operation 2, 1
    }
  }

  def void test_postcond_with_single_argument()  {
    def root = create_instance_of(source, [1])

    root.some_operation2 2
  }

  def void test_postcond_with_single_argument_and_old_var()  {
    def root = create_instance_of(source, [1])

    root.some_operation3 2                           
  }

  def void test_postcond_with_single_argument_and_old_var_fail()  {
    def root = create_instance_of(source, [1])

    shouldFail AssertionError, {
      root.some_operation3 1
    }
  }                             

  def void test_postcond_with_result_variable()  {
    def root = create_instance_of(source, [1])

    def result = root.some_operation4(1, 1)

    assertEquals 2, result
  }

  def void test_postcond_with_result_variable_and_field()  {
    def root = create_instance_of(source, [1])

    def result = root.some_operation5(1, 1)

    assertEquals 2, result
  }

  def void test_postcond_with_result_and_old_variables()  {
    def root = create_instance_of(source, [1])

    def result = root.some_operation6(2, 2)

    assertEquals 4, result
  }

  def void test_postcond_with_result_and_old_variables_switched()  {
    def root = create_instance_of(source, [1])

    def result = root.some_operation7(2, 2)

    assertEquals 4, result
  }

  def void test_postcond_with_implicit_return_statement()  {
    def root = create_instance_of(source, [1])

    def result = root.some_operation8(2)

    assertEquals 2, result
  }

  def void test_postcond_with_complex_return_statement()  {
    def root = create_instance_of(source, [1])

    def result = root.some_operation9(2, 2)

    assertEquals 4, result
  }


  def void test_multiple_preconditions()  {
    def root = create_instance_of(source, [1])

    root.some_operation11 12
  }

  def void test_multiple_precondition_fail()  {
    def root = create_instance_of(source, [1])

    shouldFail AssertionError, { root.some_operation11 10 }
  }

  def void test_multiple_postconditions()  {
    def root = create_instance_of(source, [1])

    root.some_operation10(new Date())
  }
  
}
