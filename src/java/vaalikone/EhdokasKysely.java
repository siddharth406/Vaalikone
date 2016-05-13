/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vaalikone;

import java.io.IOException;
import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import persist.Kysymykset;
import persist.Vastaukset;
import persist.VastauksetPK;
import java.lang.Math;

/**
 *
 * @author Tomi Vesanen
 */
public class EhdokasKysely implements Moduuli {

    private Kayttaja usr;
    private EntityManager em;
    private Logger logger;
    private HttpSession session;

    @Override
    public void ajaModuuli(HttpServletRequest request, HttpServletResponse response, Vaalikone vaalikone) throws ServletException, IOException {

        int kysymys_id;
        int ehdokas_id = 0;

        //hae parametrinä tuotu edellisen kysymyksen nro
        String strKysymys_id = request.getParameter("q");

        //hae parametrina tuotu edellisen kysymyksen vastaus
        String strVastaus = request.getParameter("vastaus");

        // Haetaan Vaalikoneeseen tallennetut Käyttäjä, EntityManager ja Logger
        usr = vaalikone.getUsr();
        em = vaalikone.getEm();
        logger = Logger.getLogger(Loki.class.getName());
        session = vaalikone.getSession();

        logger.log(Level.FINE, "Sessio ID: {0}", session.getId());

        // Jos kysymyksen numero (kysId) on asetettu, haetaan tuo kysymys
        // muuten haetaan kysnro 1
        if (strKysymys_id == null) {
            kysymys_id = 1;
        } else {
            kysymys_id = parseInt(strKysymys_id);
            //jos vastaus on asetettu, tallenna se session käyttäjä-olioon
            if (strVastaus != null) {
                usr.addVastaus(kysymys_id, parseInt(strVastaus));
            }

            //määritä seuraavaksi haettava kysymys
            kysymys_id++;
        }

        int kysnum = Integer.parseInt(Vaalikone.getLastId(vaalikone, "Kysymykset").toString());

        //jos kysymyksiä on vielä jäljellä, hae seuraava
        if (kysymys_id <= kysnum) {
            try {
                //Hae haluttu kysymys tietokannasta
                Query q = em.createQuery(
                        "SELECT k FROM Kysymykset k WHERE k.kysymysId=?1");
                q.setParameter(1, kysymys_id);
                //Lue haluttu kysymys listaan
                List<Kysymykset> kysymysList = q.getResultList();
                request.setAttribute("kysymykset", kysymysList);
                request.setAttribute("vaalikone", vaalikone);
                request.getRequestDispatcher("/ehdkys.jsp")
                        .forward(request, response);

            } finally {
                // Sulje tietokantayhteys
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                em.close();
            }
        } else {
            //Tallennetaan ehdokkaan vastaukset tietokantaan
            String kommentti = request.getParameter("kommentti");
            int vastaus = Integer.parseInt(request.getParameter("vastaus"));

            List<Integer> vastaukset = new ArrayList<>(kysnum);
            vastaukset = usr.getVastausLista();

            VastauksetPK vasPK = new VastauksetPK();
            Vastaukset vas = new Vastaukset();

            try {
                for (int i = 0; i < (kysnum + 1); i++) {
                    vasPK.setEhdokasId(ehdokas_id);
                    vasPK.setKysymysId(i);

                    vas.setVastauksetPK(vasPK);
                    vas.setVastaus(vastaukset.get(i));

                    em.getTransaction().begin();
                    em.persist(vasPK);
                    em.persist(vas);
                    em.getTransaction().commit();
                }
            } finally {
                if (em.getTransaction()
                        .isActive()) {
                    em.getTransaction().rollback();
                }

                em.close();
            }
        }
    }
}
