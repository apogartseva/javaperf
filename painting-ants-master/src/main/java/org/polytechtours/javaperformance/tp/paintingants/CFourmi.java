package org.polytechtours.javaperformance.tp.paintingants;
// package PaintingAnts_v3;
// version : 4.0

import java.awt.Color;
import java.util.Random;

public class CFourmi {
  /* Tableau des incrémentations à effectuer sur la position des fourmis
  en fonction de la direction du deplacement*/
  private static final int[][] mIncDirection = {
          {0, -1}, {1, -1}, {1, 0}, {1, 1},
          {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
  };

  /* le generateur aléatoire (Random est thread safe donc on la partage)*/
  private static Random GenerateurAleatoire = new Random();
  /* couleur déposé par la fourmi*/
  private Color mCouleurDeposee;
  private float mLuminanceCouleurSuivie;
  /* objet graphique sur lequel les fourmis peuvent peindre*/
  private CPainting mPainting;
  /* Coordonées de la fourmi*/
  private int x, y;
  /* Proba d'aller a gauche, en face, a droite, de suivre la couleur*/
  private float[] mProba = new float[4];
  /* Numéro de la direction dans laquelle la fourmi regarde*/
  private int mDirection;
  /* Taille de la trace de phéromones déposée par la fourmi*/
  private int mTaille;
  /* Pas d'incrémentation des directions suivant le nombre de directions
   allouées à la fourmies*/
  private int mDecalDir;
  /* l'applet*/
  private PaintingAnts mApplis;
  /* seuil de luminance pour la détection de la couleur recherchée*/
  private float mSeuilLuminance;
  /* nombre de déplacements de la fourmi*/
  private long mNbDeplacements;

  /**
   * Contructeur avec paramètres de la classe CFourmi
   * @param pCouleurDeposee
   * @param pCouleurSuivie
   * @param pProbaTD
   * @param pProbaG
   * @param pProbaD
   * @param pProbaSuivre
   * @param pPainting
   * @param pTypeDeplacement
   * @param pInit_x
   * @param pInit_y
   * @param pInitDirection
   * @param pTaille
   * @param pSeuilLuminance
   * @param pApplis
   */
  public CFourmi(Color pCouleurDeposee, Color pCouleurSuivie, float pProbaTD, float pProbaG, float pProbaD,
                 float pProbaSuivre, CPainting pPainting, char pTypeDeplacement, float pInit_x, float pInit_y, int pInitDirection,
                 int pTaille, float pSeuilLuminance, PaintingAnts pApplis) {

    mCouleurDeposee = pCouleurDeposee;
    mLuminanceCouleurSuivie = calculateLuminance(mCouleurDeposee);
    mPainting = pPainting;
    mApplis = pApplis;

    // direction de départ
    mDirection = pInitDirection;

    // taille du trait
    mTaille = pTaille;

    // initialisation des probas
    initializeProba(pProbaTD, pProbaG, pProbaD, pProbaSuivre);

    // nombre de directions pouvant être prises : 2 types de déplacement
    // possibles
    initializeIncDirection(pTypeDeplacement);

    mSeuilLuminance = pSeuilLuminance;
    mNbDeplacements = 0;

    x = modulo((int) pInit_x, pPainting.getLargeur());
    y = modulo((int) pInit_y, pPainting.getLargeur());

  }

  /**
   * Fonction de déplacement de la fourmi
   */
  public synchronized void deplacer() {
    mNbDeplacements++;

    float tirage = GenerateurAleatoire.nextFloat();
    float prob1, prob2, prob3, total;
    int[] dir = calculateDirection();
    int i, j;
    Color lCouleur;

    // la fourmi suit la couleur
    if (((tirage <= mProba[3]) && ((dir[0] + dir[1] + dir[2]) > 0)) || ((dir[0] + dir[1] + dir[2]) == 3)) {
      prob1 = (dir[0]) * mProba[0];
      prob2 = (dir[1]) * mProba[1];
      prob3 = (dir[2]) * mProba[2];
    }
    // la fourmi ne suit pas la couleur
    else {
      prob1 = (1 - dir[0]) * mProba[0];
      prob2 = (1 - dir[1]) * mProba[1];
      prob3 = (1 - dir[2]) * mProba[2];
    }
    total = prob1 + prob2 + prob3;
    prob1 = prob1 / total;
    prob2 = prob2 / total + prob1;
    prob3 = prob3 / total + prob2;

    // incrémentation de la direction de la fourmi selon la direction choisie
    float tirageDirection = GenerateurAleatoire.nextFloat();
    if (tirageDirection < prob1) {
      mDirection = modulo(mDirection - mDecalDir, 8);
    } else if (tirageDirection >= prob2){
      mDirection = modulo(mDirection + mDecalDir, 8);
    }

    x += CFourmi.mIncDirection[mDirection][0];
    y += CFourmi.mIncDirection[mDirection][1];

    x = modulo(x, mPainting.getLargeur());
    y = modulo(y, mPainting.getHauteur());

    // coloration de la nouvelle position de la fourmi
    mPainting.setCouleur(x, y, mCouleurDeposee, mTaille);

    mApplis.IncrementFpsCounter();
  }

  /**
   * Calcule et renvoie la direction
   * @return la direction
   */
  private int[] calculateDirection() {
    int[] dir = new int[3];

    for (int i = 0; i < 3; i++) {
      int nextX = modulo(x + mIncDirection[modulo(mDirection + i * mDecalDir, 8)][0], mPainting.getLargeur());
      int nextY = modulo(y + mIncDirection[modulo(mDirection + i * mDecalDir, 8)][1], mPainting.getHauteur());

      Color nextColor = (mApplis.mBaseImage != null) ?
              new Color(mApplis.mBaseImage.getRGB(nextX, nextY)) :
              new Color(mPainting.getCouleur(nextX, nextY).getRGB());

      dir[i] = testCouleur(nextColor);
    }

    return dir;
  }

  /**
   * Permet d'initialiser les probabiltiés de la fourmi à prendre une direction
   * @param probaTD
   * @param probaG
   * @param probaD
   * @param probaSuivre
   */
  private void initializeProba(float probaTD, float probaG, float probaD, float probaSuivre) {
    mProba[0] = probaG;
    mProba[1] = probaTD;
    mProba[2] = probaD;
    mProba[3] = probaSuivre;
  }

  /**
   * Permet d'initialiser le type de déplacement de la fourmi
   * @param typeDeplacement
   */
  private void initializeIncDirection(char typeDeplacement) {
    mDecalDir = (typeDeplacement == 'd') ? 2 : 1;
  }

  public long getNbDeplacements() {
    return mNbDeplacements;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }


  /**
   * Calcul du modulo permettant aux fourmi de reapparaitre de l'autre coté du Canvas
   * lorsqu'elle en sort
   * @param x
   * @param m
   * @return
   */
  private int modulo(int x, int m) {
    return (x + m) % m;
  }

  /**
   * Permet de calculer la luminance d'une couleur donnée
   * @param color
   * @return la luminance
   */
  private float calculateLuminance(Color color) {
    return 0.2426f * color.getRed() + 0.7152f * color.getGreen() + 0.0722f * color.getBlue();
  }

  /**
   * Permet de savoir si la différence de luminance entre la couleur suivi par la fourmi et la couleur trouvé
   * dépasse le seuil de luminance
   *
   * @param couleur
   * @return un boolean informant que le seuil est franchit
   */
  private int testCouleur(Color couleur) {
    float luminance = calculateLuminance(couleur);
    return (Math.abs(mLuminanceCouleurSuivie - luminance) < mSeuilLuminance) ? 1 : 0;
  }
}
