package org.gcontracts.tests.doc

import org.gcontracts.tests.basic.BaseTestClass
import org.junit.Test

/**
 * @author ast
 */
class StackExampleTests extends BaseTestClass {

  def source_stack = '''
@Contracted
package tests

import org.gcontracts.annotations.*

@Invariant({ list != null && anotherName != null })
class Stack  {

  protected def list
  def anotherName = ""
  def protected name = ""

  public Stack()  {
    this.list = []
  }

  public Stack(def list)  {
    this.list = list
  }

  @Requires({ item != null })
  @Ensures({ list[-1] == item })
  void push(def item)  {
    list.add item
  }

  @Requires({ item1 != null && item2 != null })
  void multi_push(def item1, def item2)  {
    push item1
    push item2
  }

//  @Requires({ list.size() > 0 })
//  @Ensures({ result != null })
//  def Object pop()  {
//    list[-1]
//  }

  @Ensures({ result -> result == list.size() })
  def int size()  {
    return list.size()
  }

  @Ensures({ result -> comp1 != null && comp2 != null && result > 0 })
  def int size(def comp1, comp2)  {
      return comp1 + comp2
  }

  @Ensures({ result -> result == 'tostring'})
  @Override
  def String toString()  {
    return 'tostring'
  }

  void modifyClassInvariant()  {
    anotherName = null
  }
}
'''

  def source_stack_descendant = '''
@Contracted
package tests

import org.gcontracts.annotations.*

@Invariant({ count >= 0 })
class StackDescendant extends Stack implements Serializable {

  def private int count = 0

  def StackDescendant() {
     super()
  }

  def StackDescendant(list) {
    super(list)
  }

  @Override
  void push(def item)  {
    count++
    super.push item
  }

  void push_fail(def item)  {
    count++
    list = null
  }

  @Ensures({ old -> old.count < count })
  void test_count()  {
    count++
  }

  @Ensures({ result, old -> true })
  def int test_count_with_result_variable()  {
    count++
    return count
  }
}
'''

  @Test void creation()  {
    create_instance_of(source_stack)
    create_instance_of(source_stack_descendant)
  }

  @Test void inherited_invariant()  {
    create_instance_of(source_stack)
    def stack = create_instance_of(source_stack_descendant)

    stack.push 'item 1'
  }


  @Test void inherited_invariant_failure()  {
    create_instance_of(source_stack)

    shouldFail AssertionError, {
      create_instance_of(source_stack_descendant, [null])
    }
  }

  @Test void inherited_invariant_fail_on_method_call()  {
    create_instance_of(source_stack)
    def stack = create_instance_of(source_stack_descendant)

    shouldFail AssertionError, {
      stack.push_fail 'item 1'
    }
  }

  @Test void old_variable()  {
    create_instance_of(source_stack)
    def stack = create_instance_of(source_stack_descendant)

    stack.test_count()
  }


  @Test void old_and_result_variable()  {
    create_instance_of(source_stack)
    def stack = create_instance_of(source_stack_descendant)

    stack.test_count_with_result_variable()
  }

}
