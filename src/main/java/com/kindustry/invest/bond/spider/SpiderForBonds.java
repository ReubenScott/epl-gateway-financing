package com.kindustry.invest.bond.spider;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.kindustry.common.date.DateUtil;
import com.kindustry.common.http.JsoupUtil;
import com.kindustry.common.util.StringUtil;
import com.kindustry.invest.bond.model.Bond;

public class SpiderForBonds {

  // 通过 集思录 取 债券 数据
  public List<Bond> getBonds() {
    List<Bond> bonds = new ArrayList<Bond>();
    String url = "https://www.jisilu.cn/data/bond/?do_search=true&forall=1&treasury=0&from_issuer_rating=1&from_volume=0&to_issuer_rating=99";
    Date sysDate = DateUtil.getCurrentShortDate();
    Document doc = JsoupUtil.getWebDocument(url);
    Elements table = doc.select("table").eq(3); // 取债券数据表格
    Elements trs = table.select("tr"); // 取行数据

    for (int i = 2; i < trs.size(); i++) {
      Elements tds = trs.get(i).select("td"); // 获取每一行的列数据
      Bond bond = new Bond();
      bond.setCode(tds.get(0).text()); // 
      bond.setName(tds.get(1).text()); // 
      bond.setNetPrice(Float.valueOf(tds.get(2).text())); // 
      bond.setFullPrice(Float.valueOf(tds.get(3).text())); // 

      if (bond.getName().contains("PR") || bond.getNetPrice() < 90f) {
        float balance = getBondBalance(bond.getCode());
        if (balance > 0) {
          bond.setFacePrice(balance);
        }
      } else {
        bond.setFacePrice(100f); // 默认面值为 100 元
      }

      bond.setAccruedInterest(Float.valueOf(tds.get(3).text()) - Float.valueOf(tds.get(2).text())); // 
      bond.setTurnVolume(Float.valueOf(tds.get(5).text())); // 
      if (!StringUtil.isEmpty(tds.get(6).text())) {
        bond.setInpaydays(Integer.valueOf(tds.get(6).text())); // 
      }
      bond.setCouponRate(Float.valueOf(tds.get(11).text())); // 
      bond.setBondCredit(tds.get(14).text()); // 
      bond.setMainCredit(tds.get(15).text()); // 
      bond.setAssukind(tds.get(16).text()); // 
      bond.setDueDate(DateUtil.parseShortDate(tds.get(17).text())); // 
      bond.setAmount(Float.valueOf(tds.get(18).text())); // 

      // for (int j = 0; j < tds.size(); j++) {
      // String oldClose = tds.get(j).text();
      // System.out.println(oldClose);
      // }

      bond.setSourceDate(sysDate);
      bond.setRecordDate(sysDate);
      bonds.add(bond);

    }

    return bonds;
  }

  // 通过 债券现金流 计算 债券剩余面值
  // http://basic.10jqka.com.cn/122695/dividend.html
  public float getBondBalance(String bondcode) {
    float balance = 0f;
    try {
      String url = "http://basic.10jqka.com.cn/" + bondcode + "/dividend.html";
      // Document doc = Jsoup.connect(url).timeout(5000).get();
      Document doc = Jsoup.connect(url).get();
      Elements table = doc.select("table").eq(0); // 取第一张表格 债券现金流
      Elements trs = table.select("tr"); // 取行数据

      Date today = Calendar.getInstance().getTime(); // 当前日期
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

      for (int i = 0; i < trs.size(); i++) {
        Elements tds = trs.get(i).select("td"); // 获取每一行的列数据
        String datetext = tds.eq(0).text();
        String honorcashflow = tds.eq(3).text();
        if (StringUtil.isEmpty(datetext)) { // 表头
          continue;
        }
        float honor = 0f;
        if (!StringUtil.isEmpty(honorcashflow.trim())) { // 表头
          honor = Float.valueOf(tds.eq(3).text());
        }

        Date statdate = dateFormat.parse(datetext.trim()); // 现金流日期

        // 比较日期
        if (statdate.getTime() > today.getTime()) {
          balance += honor;
        }
      }

      // System.out.println(today + "  " + balance );
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return balance;
  }

}
