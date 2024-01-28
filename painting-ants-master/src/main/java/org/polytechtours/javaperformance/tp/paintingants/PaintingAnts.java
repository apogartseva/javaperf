package org.polytechtours.javaperformance.tp.paintingants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.Timer;

public class PaintingAnts extends java.applet.Applet implements Runnable {
  private static final long serialVersionUID = 1L;
  // parametres
  private int mLargeur;
  private int mHauteur;

  // l'objet graphique lui meme
  private CPainting mPainting;

  // les fourmis
  private Vector<CFourmi> mColonie = new Vector<CFourmi>();
  private CColonie mColony;

  private Thread mApplis, mThreadColony;

  private Dimension mDimension;
  private long mCompteur = 0;
  private Object mMutexCompteur = new Object();
  private boolean mPause = false;

  public BufferedImage mBaseImage;
  private Timer fpsTimer;

  /** Fourmis per second */
  private Long fpsCounter = 0L;
  /** stocke la valeur du compteur lors du dernier timer */
  private Long lastFps = 0L;

  /**
   * incrémenter le compteur
   *
   */
  public void compteur() {
    synchronized (mMutexCompteur) {
      mCompteur++;
    }
  }

  /**
   * Détruire l'applet
   *
   */
  @Override
  public void destroy() {
    if (mApplis != null) {
      mApplis = null;
    }
  }

  /**
   * Obtenir l'information Applet
   *
   */
  @Override
  public String getAppletInfo() {
    return "Painting Ants";
  }

  /**
   * Obtenir l'information Applet
   *
   */
  @Override
  public String[][] getParameterInfo() {
    String[][] lInfo = { { "SeuilLuminance", "string", "Seuil de luminance" }, { "Img", "string", "Image" },
        { "NbFourmis", "string", "Nombre de fourmis" }, { "Fourmis", "string",
            "Paramètres des fourmis (RGB_déposée)(RGB_suivie)(x,y,direction,taille)(TypeDeplacement,ProbaG,ProbaTD,ProbaD,ProbaSuivre);...;" } };
    return lInfo;
  }

  /**
   * Obtenir l'état de pause
   *
   */
  public boolean getPause() {
    return mPause;
  }

  public synchronized void IncrementFpsCounter() {
    fpsCounter++;
  }

  /**
   * Initialisation de l'applet
   *
   */
  @Override
  public void init() {
    URL lFileName;
    URLClassLoader urlLoader = (URLClassLoader) this.getClass().getClassLoader();

    // lecture des parametres de l'applet

    mDimension = getSize();
    mLargeur = mDimension.width;
    mHauteur = mDimension.height;

    mPainting = new CPainting(mDimension, this);
    add(mPainting);

    // lecture de l'image
    lFileName = urlLoader.findResource("images/" + getParameter("Img"));
    try {
      if (lFileName != null) {
        mBaseImage = javax.imageio.ImageIO.read(lFileName);
      }
    } catch (java.io.IOException ex) {
    }

    if (mBaseImage != null) {
      mLargeur = mBaseImage.getWidth();
      mHauteur = mBaseImage.getHeight();
      mDimension.setSize(mLargeur, mHauteur);
      resize(mDimension);
    }

    readParameterFourmis();

    setLayout(null);
  }

  /**
   * Paint the image and all active highlights.
   */
  @Override
  public void paint(Graphics g) {
    if (mBaseImage != null) {
      g.drawImage(mBaseImage, 0, 0, this);
    }
  }

  /**
   * Mettre en pause
   *
   */
  public void pause() {
    mPause = !mPause;
  }

  /**
   * Retourne pStr sous la forme d'un float si s'en est un
   * Retourne une caleur au hasard dans l'intervale pStr si s'en est un
   * @param pStr
   * @return un float correspondant à pStr
   */
  private float readFloatParameter(String pStr) {
    float lMin, lMax, lResult;
    StringTokenizer lStrTok = new StringTokenizer(pStr, ":");
    // on lit une premiere valeur
    lMin = Float.valueOf(lStrTok.nextToken()).floatValue();
    lResult = lMin;
    // on essaye d'en lire une deuxieme
    try {
      lMax = Float.valueOf(lStrTok.nextToken()).floatValue();
      if (lMax > lMin) {
        // on choisit un nombre entre lMin et lMax
        lResult = (float) (Math.random() * (lMax - lMin)) + lMin;
      }
    } catch (java.util.NoSuchElementException e) {
      // il n'y pas de deuxieme nombre et donc le nombre retourné correspond au
      // premier nombre
    }
    return lResult;
  }

  /**
   * Retourne pStr sous la forme d'un nombre si s'en est un
   * Retourne une caleur au hasard dans l'intervale pStr si s'en est un
   * @param pStr
   * @return un entier correspondant à pStr
   */
  private int readIntParameter(String pStr) {
    int lMin, lMax, lResult;
    StringTokenizer lStrTok = new StringTokenizer(pStr, ":");
    // on lit une premiere valeur
    lMin = Integer.valueOf(lStrTok.nextToken()).intValue();
    lResult = lMin;
    // on essaye d'en lire une deuxieme
    try {
      lMax = Integer.valueOf(lStrTok.nextToken()).intValue();
      if (lMax > lMin) {
        // on choisit un nombre entre lMin et lMax
        lResult = (int) (Math.random() * (lMax - lMin + 1)) + lMin;
      }
    } catch (java.util.NoSuchElementException e) {
      // il n'y pas de deuxieme nombre et donc le nombre retourné correspond au
      // premier nombre
    }
    return lResult;
  }

  /**
   * Lecture des paramètres de l'applet
   */
  private void readParameterFourmis() {
    float lSeuilLuminance = readSeuilLuminance();
    System.out.println("Seuil de luminance:" + lSeuilLuminance);

    int lNbFourmis = readNbFourmis();
    if (lNbFourmis == -1) {
      lNbFourmis = (int) (Math.random() * 5) + 2;
    }

    String lChaine = getParameter("Fourmis");
    if (lChaine != null) {
      System.out.println("Paramètres:" + lChaine);

      lNbFourmis = 0;
      StringTokenizer lSTFourmi = new StringTokenizer(lChaine, ";");
      while (lSTFourmi.hasMoreTokens()) {
        String params = lSTFourmi.nextToken();
        processFourmiParameters(params, lNbFourmis, lSeuilLuminance);
        lNbFourmis++;
      }
    } else {
      initializeRandomFourmis(lNbFourmis,lSeuilLuminance);
    }
  }

  /**
   * Lit le seuil de luminance
   * @return le seuil de luminance
   */
  private float readSeuilLuminance() {
    String lChaine = getParameter("SeuilLuminance");
    return (lChaine != null) ? readFloatParameter(lChaine) : 40f;
  }

  /**
   * Lit le nombre de fourmis
   * @return le nombre de fourmis
   */
  private int readNbFourmis() {
    String lChaine = getParameter("NbFourmis");
    return (lChaine != null) ? readIntParameter(lChaine) : -1;
  }

  /**
   * Traite les paramètres d'une fourmi
   * @param params
   * @param lNbFourmis
   * @param lSeuilLuminance
   */
  private void processFourmiParameters(String params, int lNbFourmis, float lSeuilLuminance) {
    StringTokenizer lSTParam = new StringTokenizer(params, "()");
    Color lCouleurDeposee = readColor(lSTParam.nextToken());
    Color lCouleurSuivie = readColor(lSTParam.nextToken());
    float lInit_x = readFloatParameter(lSTParam.nextToken());
    float lInit_y = readFloatParameter(lSTParam.nextToken());
    int lInitDirection = readIntParameter(lSTParam.nextToken());
    int lTaille = readIntParameter(lSTParam.nextToken());
    char lTypeDeplacement = readTypeDeplacement(lSTParam.nextToken());
    float lProbaG = readFloatParameter(lSTParam.nextToken());
    float lProbaTD = readFloatParameter(lSTParam.nextToken());
    float lProbaD = readFloatParameter(lSTParam.nextToken());
    float lProbaSuivre = readFloatParameter(lSTParam.nextToken());

    normalizeProbas(lProbaG, lProbaTD, lProbaD);

    System.out.println("(" + lTypeDeplacement + "," + lProbaG + "," + lProbaTD + "," + lProbaD + "," + lProbaSuivre + ");");

    CFourmi lFourmi = new CFourmi(lCouleurDeposee, lCouleurSuivie, lProbaTD, lProbaG, lProbaD, lProbaSuivre, mPainting,
            lTypeDeplacement, lInit_x, lInit_y, lInitDirection, lTaille, lSeuilLuminance, this);
    mColonie.addElement(lFourmi);
  }

  /**
   * Initialise aléatoirement des fourmis et leur paramètres
   * @param nbFourmis
   * @param lSeuilLuminance
   */
  private void initializeRandomFourmis(int nbFourmis, float lSeuilLuminance) {
    for (int i = 0; i < nbFourmis; i++) {
      int R = (int) (Math.random() * 256);
      int G = (int) (Math.random() * 256);
      int B = (int) (Math.random() * 256);
      Color couleurDeposee = new Color(R, G, B);

      int colorIndex = (int) (Math.random() * nbFourmis);
      if (i == colorIndex) {
        colorIndex = (colorIndex + 1) % nbFourmis;
      }
      Color couleurSuivie = new Color((int) (Math.random() * 256), (int) (Math.random() * 256),
              (int) (Math.random() * 256));

      char typeDeplacement = (float) Math.random() < 0.5f ? 'd' : 'o';

      float init_x = (float) Math.random();
      float init_y = (float) Math.random();

      int initDirection = (int) (Math.random() * 8);
      int taille = (int) (Math.random() * 4);

      float probaTD = (float) Math.random();
      float probaG = (float) (Math.random() * (1.0 - probaTD));
      float probaD = (float) (1.0 - (probaTD + probaG));
      float probaSuivre = (float) (0.5 + 0.5 * Math.random());

      System.out.print("Random:(" + R + "," + G + "," + B + ")");
      System.out.print("(" + couleurSuivie.getRed() + "," + couleurSuivie.getGreen() + "," + couleurSuivie.getBlue()
              + ")");
      System.out.print("(" + init_x + "," + init_y + "," + initDirection + "," + taille + ")");
      System.out.println("(" + typeDeplacement + "," + probaG + "," + probaTD + "," + probaD + "," + probaSuivre + ");");

      CFourmi fourmi = new CFourmi(couleurDeposee, couleurSuivie, probaTD, probaG, probaD, probaSuivre, mPainting,
              typeDeplacement, init_x, init_y, initDirection, taille, lSeuilLuminance, this);
      mColonie.addElement(fourmi);
    }
  }

  /**
   * Lit une couleur sous la forme d'un string et la traduit en objet Color
   * @param colorParams
   * @return la couleur sous forme Color
   */
  private Color readColor(String colorParams) {
    StringTokenizer lSTCouleur = new StringTokenizer(colorParams, ",");
    int R = readIntParameter(lSTCouleur.nextToken());
    int G = readIntParameter(lSTCouleur.nextToken());
    int B = readIntParameter(lSTCouleur.nextToken());
    return new Color(R, G, B);
  }

  /**
   * Lit le type de déplacement
   * @param typeDeplacementParam
   * @return le type de déplacement
   */
  private char readTypeDeplacement(String typeDeplacementParam) {
    char lTypeDeplacement = typeDeplacementParam.charAt(0);
    if (lTypeDeplacement != 'o' && lTypeDeplacement != 'd') {
      lTypeDeplacement = (Math.random() < 0.5) ? 'o' : 'd';
    }
    return lTypeDeplacement;
  }

  /**
   * Normalise les probabilités de déplacement
   * @param probaG
   * @param probaTD
   * @param probaD
   */
  private void normalizeProbas(float probaG, float probaTD, float probaD) {
    float lSomme = probaG + probaTD + probaD;
    probaG /= lSomme;
    probaTD /= lSomme;
    probaD /= lSomme;
  }



  @Override
  public void run() {
    String lMessage;

    mPainting.init();

    Thread currentThread = Thread.currentThread();

    mThreadColony.start();

    while (mApplis == currentThread) {
      if (mPause) {
        lMessage = "pause";
      } else {
        synchronized (this) {
          lMessage = "running (" + lastFps + ") ";
        }

        synchronized (mMutexCompteur) {
          mCompteur %= 10000;
          for (int i = 0; i < mCompteur / 1000; i++) {
            lMessage += ".";
          }
        }

      }
      showStatus(lMessage);

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        showStatus(e.toString());
      }
    }
  }

  /**
   * Lancer l'applet
   *
   */
  @Override
  public void start() {
    mColony = new CColonie(mColonie, this);
    mThreadColony = new Thread(mColony);
    mThreadColony.setPriority(Thread.MIN_PRIORITY);

    fpsTimer = new Timer(1000, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateFPS();
      }
    });
    fpsTimer.setRepeats(true);
    fpsTimer.start();

    showStatus("starting...");
    // Create the thread.
    mApplis = new Thread(this);
    // and let it start running
    mApplis.setPriority(Thread.MIN_PRIORITY);
    mApplis.start();
  }

  /**
   * Arrêter l'applet
   *
   */
  @Override
  public void stop() {
    showStatus("stopped...");

    fpsTimer.stop();

    // On demande au Thread Colony de s'arreter et on attend qu'il s'arrete
    mColony.pleaseStop();
    try {
      mThreadColony.join();
    } catch (Exception e) {
    }

    mThreadColony = null;
    mApplis = null;
  }

  /**
   * update Fourmis per second
   */
  private synchronized void updateFPS() {
    lastFps = fpsCounter;
    fpsCounter = 0L;
  }
}
