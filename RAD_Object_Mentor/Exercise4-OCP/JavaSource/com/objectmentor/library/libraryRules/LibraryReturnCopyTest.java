package com.objectmentor.library.libraryRules;

import com.objectmentor.library.mocks.*;
import com.objectmentor.library.models.*;
import com.objectmentor.library.utils.DateUtil;
import junit.framework.*;

import java.util.*;

public class LibraryReturnCopyTest extends TestCase {
  private Library library;
  private String copyId;
  private MediaCopy copy;
  private Patron david;

  protected void setUp() throws Exception {
    MockIsbnService isbnService = new MockIsbnService();
    library = new Library(isbnService, null, new MockMediaGateway(), new MockPatronGateway(), new MockCardPrinter(), null);
    isbnService.setBookToReturn(new Media("isbn", "War and Peace", "Tolstoy"));
    copy = library.acceptBook("isbn");
    copyId = copy.getId();
    david = new Patron(DateUtil.dateFromString("1/1/2000"));
    library.getPatronGateway().add(david);
  }

  public void testCanReturnOnTimeBookThatWasBorrowed() throws Exception {
    library.loan(copyId, david.getId());
    ReturnReceipt returnReceipt = library.returnCopy(copyId);
    Assert.assertEquals(new Money(0), returnReceipt.getFines());
    assertEquals(copy, returnReceipt.getCopy());
    assertFalse(returnReceipt.getCopy().isLoaned());
    assertEquals(ReturnReceipt.OK, returnReceipt.getStatus());
    Map<String, Money> charges = returnReceipt.getCharges();
    assertTrue(charges.isEmpty());
  }

  public void testShouldSetStatusToUnknownBookOnAttemptToReturnUnknownBook() throws Exception {
    ReturnReceipt returnReceipt = library.returnCopy("unknown terrible string");
    assertEquals(ReturnReceipt.UNKNOWN_BOOK, returnReceipt.getStatus());
  }

  public void testShouldSetStatusToUnBorrowedBookOnAttemptToReturnUnBorrowedBook() throws Exception {
    ReturnReceipt returnReceipt = library.returnCopy(copyId);
    assertEquals(ReturnReceipt.UNBORROWED_BOOK, returnReceipt.getStatus());
    assertEquals(copy, returnReceipt.getCopy());
  }

  public void testShouldHaveFineAndBeLateIfOneDayLate() throws Exception {
    Date borrowDate = DateUtil.dateFromString("12/19/2006");
    Date returnDate = DateUtil.dateFromString("1/3/2007"); // fifteen days later.
    TimeSource.timeSource = new MockTimeSource(borrowDate);
    library.loan(copyId, david.getId());
    TimeSource.timeSource = new MockTimeSource(returnDate);
    ReturnReceipt receipt = library.returnCopy(copyId);

    assertEquals(ReturnReceipt.LATE, receipt.getStatus());
    Assert.assertEquals(new Money(50), receipt.getFines());
  }


  public void testShouldHaveFineAndBeLateIfTwoDaysLate() throws Exception {
    Date borrowDate = DateUtil.dateFromString("12/19/2006");
    Date returnDate = DateUtil.dateFromString("1/4/2007"); // sixteen days later.
    TimeSource.timeSource = new MockTimeSource(borrowDate);
    library.loan(copyId, david.getId());
    TimeSource.timeSource = new MockTimeSource(returnDate);
    ReturnReceipt receipt = library.returnCopy(copyId);

    assertEquals(ReturnReceipt.LATE, receipt.getStatus());
    Assert.assertEquals(new Money(100), receipt.getFines());
  }

  public void testChargeIfDamaged() throws Exception {
    library.loan(copyId, david.getId());
    ReturnCondition damaged = new DamagedCondition();
    List<ReturnCondition> conditions = new ArrayList<ReturnCondition>();
    conditions.add(damaged);
    ReturnReceipt returnReceipt = library.returnCopy(copyId, conditions);
    Map<String, Money> charges = returnReceipt.getCharges();
    String conditionName = damaged.getConditionName();
    assertTrue(charges.containsKey(conditionName));
    assertEquals(new Money(500), charges.get(conditionName));
  }


}
