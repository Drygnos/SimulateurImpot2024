package com.kerware.simulateur;

public class AdaptateurSimulateur implements ICalculateurImpot {

    private Simulateur simulateur = new Simulateur();

    private int revenusNetDecl1 = 0;
    private int revenusNetDecl2 = 0;
    private SituationFamiliale situationFamiliale;
    private int nbEnfantsACharge;
    private int nbEnfantsSituationHandicap;
    private boolean parentIsole;


    @Override
    public void setRevenusNetDeclarant1(int rn) {
        if (rn < 0) {
            throw new IllegalArgumentException("Le revenu net du déclarant 1 ne peut pas être négatif.");
        }
        this.revenusNetDecl1 = rn;
        System.out.println("Revenu net déclarant 1 mis à jour : " + rn);
    }

    @Override
    public void setRevenusNetDeclarant2(int rn) {
        if (rn < 0) {
            throw new IllegalArgumentException("Le revenu net du déclarant 2 ne peut pas être négatif.");
        }
        this.revenusNetDecl2 = rn;
        System.out.println("Revenu net déclarant 2 mis à jour : " + rn);
    }

    @Override
    public void setSituationFamiliale(SituationFamiliale sf) {
        if (sf == null) {
            throw new IllegalArgumentException("La situation familiale ne peut pas être null.");
        }
        this.situationFamiliale = sf;
        System.out.println("Situation familiale mise à jour : " + sf);
    }

    @Override
    public void setNbEnfantsACharge(int nbe) {
        if (nbe < 0) {
            throw new IllegalArgumentException("Le nombre d'enfants à charge ne peut pas être négatif.");
        } else if (nbe > 7) {
            throw new IllegalArgumentException("Le nombre d'enfants à charge ne peut pas dépasser 7.");
        }
        this.nbEnfantsACharge = nbe;
        System.out.println("Nombre d'enfants à charge mis à jour : " + nbe);
    }

    @Override
    public void setNbEnfantsSituationHandicap(int nbesh) {
        if (nbesh < 0) {
            throw new IllegalArgumentException("Le nombre d'enfants en situation de handicap ne peut pas être négatif.");
        } else if (nbesh > 7) {
            throw new IllegalArgumentException("Le nombre d'enfants en situation de handicap ne peut pas dépasser 7.");
        }
        this.nbEnfantsSituationHandicap = nbesh;
        System.out.println("Nombre d'enfants en situation de handicap mis à jour : " + nbesh);
    }

    @Override
    public void setParentIsole(boolean pi) {
        if (pi && (situationFamiliale == SituationFamiliale.MARIE || situationFamiliale == SituationFamiliale.PACSE)) {
            throw new IllegalArgumentException("Un parent isolé ne peut pas être en situation de couple.");
        }
        this.parentIsole = pi;
        System.out.println("Statut de parent isolé mis à jour : " + pi);
    }

    @Override
    public void calculImpotSurRevenuNet() {
        System.out.println("Calcul de l'impôt avec les paramètres :");
        System.out.println("Revenu déclarant 1 : " + revenusNetDecl1);
        System.out.println("Revenu déclarant 2 : " + revenusNetDecl2);
        System.out.println("Situation familiale : " + situationFamiliale);
        System.out.println("Nombre d'enfants à charge : " + nbEnfantsACharge);
        System.out.println("Nombre d'enfants en situation de handicap : " + nbEnfantsSituationHandicap);
        System.out.println("Parent isolé : " + parentIsole);

        try {
            simulateur.calculImpot(revenusNetDecl1, revenusNetDecl2, situationFamiliale, nbEnfantsACharge, nbEnfantsSituationHandicap, parentIsole);
            System.out.println("Calcul de l'impôt réussi.");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Erreur dans les paramètres d'entrée : " + e.getMessage());
        }
    }

    @Override
    public int getRevenuNetDeclatant1() {
        return revenusNetDecl1;
    }

    @Override
    public int getRevenuNetDeclatant2() {
        return revenusNetDecl2;
    }

    @Override
    public double getContribExceptionnelle() {
        return simulateur.getContribExceptionnelle();
    }

    @Override
    public int getRevenuFiscalReference() {
        return (int)simulateur.getRevenuReference();
    }

    @Override
    public int getAbattement() {
        return (int)simulateur.getAbattement();
    }

    @Override
    public double getNbPartsFoyerFiscal() {
        return simulateur.getNbParts();
    }

    @Override
    public int getImpotAvantDecote() {
        return (int)simulateur.getImpotAvantDecote();
    }

    @Override
    public int getDecote() {
        return (int)simulateur.getDecote();
    }

    @Override
    public int getImpotSurRevenuNet() {
        return (int)simulateur.getImpotNet();
    }
}
