package models.book;


import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import models.deliverypoint.DeliveryPoint;
import models.user.LibraryUser;
import models.user.UserFine;
import utils.Assistant;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mistler on 19.05.16.
 */
@Entity
public class TakenBook extends Model {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @NotNull
    public LibraryUser libraryUser;

    @NotNull
    public String takenBookStatus;

    @ManyToOne
    @NotNull
    public BookInstance bookInstance;

    @NotNull
    public Date takeDate;

    @NotNull
    public Date returnDate;

    public static List<TakenBook> all(){
        return new Model.Finder(String.class, TakenBook.class).all();
    }

    public static TakenBook byId(Object id){
        return (TakenBook) new Finder(String.class, TakenBook.class).byId(id);
    }

    public TakenBook(LibraryUser user, String status, BookInstance instance, Date takeDate, Date returnDate){
        this.libraryUser = user;
        this.takenBookStatus = status;
        this.bookInstance = instance;
        this.takeDate = takeDate;
        this.returnDate = returnDate;
    }

    @Override
    public String toString(){
        Book book = bookInstance.book;
        DeliveryPoint deliveryPoint = bookInstance.deliveryPoint;
        return "Title: " + book.title + ", Author: " + book.author + ", Take: " + Assistant.dateToString(takeDate) + ", Return: " +
                Assistant.dateToString(returnDate) + " (Delivery point: " +
                deliveryPoint.name + ", " + deliveryPoint.address + ": " + deliveryPoint.deliveryPointType.name + ")";
    }

    // TRIGGER!
    @Override
    public void update(){
        super.save();
        String newStatus = takenBookStatus;
        switch(newStatus){
            case "Returned":
                Date today = Assistant.today();
                if(returnDate.before(today)){
                    UserFine fine = new UserFine(libraryUser, today, Assistant.nextDay(10), null);
                    fine.save();
                }
                break;
            case "Lost":
                UserFine fine = new UserFine(libraryUser, null, null, bookInstance.book.price * 10);
                fine.save();
                break;
        }
    }

    public static List<TakenBook> byUserCurrently(LibraryUser libraryUser){
        String sql = "SELECT taken_book.id AS id FROM taken_book\n" +
                "WHERE taken_book.library_user_id = :libraryUserId AND taken_book_status like 'User'";
        SqlQuery query = Ebean.createSqlQuery(sql);
        query.setParameter("libraryUserId", libraryUser.id);
        List<SqlRow> sqlRows = query.findList();
        List<TakenBook> ret = new ArrayList<>();
        for (SqlRow row : sqlRows) {
            Long id = (Long)row.get("id");
            TakenBook takenBook = (TakenBook) new Finder(String.class, TakenBook.class).byId(id);
            ret.add(takenBook);
        }
        return ret;
    }
}
