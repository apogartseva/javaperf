package org.polytechtours.javaperformance.tp.paintingants;
// package PaintingAnts_v2;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;

// version : 2.0

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Nicolas Monmarché
 * @version 1.0
 */

public class CPainting extends Canvas implements MouseListener {
  private static final long serialVersionUID = 1L;
  static private float[][] mMatriceConv9 = new float[3][3];
  static private float[][] mMatriceConv25 = new float[5][5];
  static private float[][] mMatriceConv49 = new float[7][7];

  // Matrices de numérateurs statiques
  private static final float[][] numerateurs9 = {
          {1, 2, 1},
          {2, 4, 2},
          {1, 2, 1}
  };

  private static final float[][] numerateurs25 = {
          {1, 1, 2, 1, 1},
          {1, 2, 3, 2, 1},
          {2, 3, 4, 3, 2},
          {1, 2, 3, 2, 1},
          {1, 1, 2, 1, 1}
  };

  private static final float[][] numerateurs49 = {
          {1, 1, 2, 2, 2, 1, 1},
          {1, 2, 3, 4, 3, 2, 1},
          {2, 3, 4, 5, 4, 3, 2},
          {2, 4, 5, 8, 5, 4, 2},
          {2, 3, 4, 5, 4, 3, 2},
          {1, 2, 3, 4, 3, 2, 1},
          {1, 1, 2, 2, 2, 1, 1}
  };

  /* Objet de type Graphics permettant de manipuler l'affichage du Canvas*/
  private Graphics mGraphics;
  /* Objet ne servant que pour les bloc synchronized pour la manipulation du
   tableau des couleurs*/
  private Object mMutexCouleurs = new Object();
  /* tableau des couleurs, il permert de conserver en memoire l'état de chaque
   pixel du canvas, ce qui est necessaire au deplacemet des fourmi
   il sert aussi pour la fonction paint du Canvas*/
  private Color[][] mCouleurs;
  /* couleur du fond*/
  private Color mCouleurFond = new Color(255, 255, 255);
  /* dimensions*/
  private Dimension mDimension = new Dimension();

  private PaintingAnts mApplis;

  private boolean mSuspendu = false;

  /**
   * Constructeur avec paramètres de la classe CPainting
   * @param pDimension
   * @param pApplis
   */
  public CPainting(Dimension pDimension, PaintingAnts pApplis) {
    int i, j;
    addMouseListener(this);

    mApplis = pApplis;

    mDimension = pDimension;
    setBounds(new Rectangle(0, 0, mDimension.width, mDimension.height));

    this.setBackground(mCouleurFond);

    // initialisation de la matrice des couleurs
    mCouleurs = new Color[mDimension.width][mDimension.height];
    synchronized (mMutexCouleurs) {
      for (i = 0; i != mDimension.width; i++) {
        for (j = 0; j != mDimension.height; j++) {
          mCouleurs[i][j] = new Color(mCouleurFond.getRed(), mCouleurFond.getGreen(), mCouleurFond.getBlue());
        }
      }
    }
  }

  /**
   * Permet d'initialiser les matrices de convolutions avec un lissage moyen
   */
  private void initMatricesConvolution() {
    initMatriceConv(mMatriceConv9,numerateurs9,16);
    initMatriceConv(mMatriceConv25,numerateurs25,44);
    initMatriceConv(mMatriceConv49, numerateurs49,128);
  }

  /**
   * Permet d'initialiser une matrice de convolution avec un lissage moyen
   * @param matrix
   * @param matriceNumerateur
   * @param denominateur
   */
  private static void initMatriceConv(float[][] matrix,float[][] matriceNumerateur, int denominateur) {
    for (int i = 0; i < matrix.length; i++) {
      for (int j = 0; j < matrix.length; j++) {
        matrix[i][j] = matriceNumerateur[i][j] / denominateur;
      }
    }
  }

  /**
   * Recupère la couleur d'une case
   * @param x
   * @param y
   * @return couleur de la case
   */
  public Color getCouleur(int x, int y) {
    synchronized (mMutexCouleurs) {
      return mCouleurs[x][y];
    }
  }

  public Dimension getDimension() {
    return mDimension;
  }

  public int getHauteur() {
    return mDimension.height;
  }

  public int getLargeur() {
    return mDimension.width;
  }

  /**
   * Initialise la canvas à blanc
   */
  public void init() {
    int i, j;
    mGraphics = getGraphics();
    synchronized (mMutexCouleurs) {
      mGraphics.clearRect(0, 0, mDimension.width, mDimension.height);

      // initialisation de la matrice des couleurs
      for (i = 0; i != mDimension.width; i++) {
        for (j = 0; j != mDimension.height; j++) {
          mCouleurs[i][j] = new Color(mCouleurFond.getRed(), mCouleurFond.getGreen(), mCouleurFond.getBlue());
        }
      }
    }
    initMatricesConvolution();
    mSuspendu = false;
  }

  /**
   * Défini le comportement associé au clic de la souris
   * @param pMouseEvent the event to be processed
   */
  public void mouseClicked(MouseEvent pMouseEvent) {
    pMouseEvent.consume();
    if (pMouseEvent.getButton() == MouseEvent.BUTTON1) {
      // double clic sur le bouton gauche = effacer et recommencer
      if (pMouseEvent.getClickCount() == 2) {
        init();
      }
      // simple clic = suspendre les calculs et l'affichage
      mApplis.pause();
    } else if (pMouseEvent.getButton() == MouseEvent.BUTTON2) {
      suspendre();
    } else {
      // clic bouton droit = effacer et recommencer
      // case pMouseEvent.BUTTON3:
      init();
    }
  }

  public void mouseEntered(MouseEvent pMouseEvent) {
  }

  public void mouseExited(MouseEvent pMouseEvent) {
  }

  public void mousePressed(MouseEvent pMouseEvent) {

  }

  public void mouseReleased(MouseEvent pMouseEvent) {
  }

  /**
   * Redessine le composant
   * @param pGraphics   the specified Graphics context
   */
  @Override
  public void paint(Graphics pGraphics) {
    synchronized (mMutexCouleurs) {
      for (int i = 0; i < mDimension.width; i++) {
        for (int j = 0; j < mDimension.height; j++) {
          pGraphics.setColor(mCouleurs[i][j]);
          pGraphics.fillRect(i, j, 1, 1);
        }
      }
    }
  }

  /**
   * Colore le pixel en paramètre et met à jour le tableau des couleurs
   * @param x
   * @param y
   * @param c
   * @param pTaille
   */
  public void setCouleur(int x, int y, Color c, int pTaille) {

    synchronized (mMutexCouleurs) {
      if (!mSuspendu) {
        // on colorie la case sur laquelle se trouve la fourmi
        mGraphics.setColor(c);
        mGraphics.fillRect(x, y, 1, 1);
      }

      mCouleurs[x][y] = c;

      // on fait diffuser la couleur :
      switch (pTaille) {
        case 0:
          // on ne fait rien = pas de diffusion
          break;
        case 1:
          // produit de convolution discrete sur 9 cases
          applyConvolution(x, y, CPainting.mMatriceConv9, 3, 3, 2, 2, 1, 1);
          break;
        case 2:
          // produit de convolution discrete sur 25 cases
          applyConvolution(x, y, CPainting.mMatriceConv25, 5, 5, 4, 4, 2, 2);
          break;
        case 3:
          // produit de convolution discrete sur 49 cases
          applyConvolution(x, y, CPainting.mMatriceConv49, 7, 7, 6, 6, 3, 3);
          break;
      }
    }
  }

  /**
   * Permet d'applicaquer la convolution sur les matrices
   * @param x
   * @param y
   * @param matriceConv
   * @param tailleX
   * @param tailleY
   * @param offsetX
   * @param offsetY
   * @param offsetM
   * @param offsetN
   */
  private void applyConvolution(int x, int y, float[][] matriceConv, int tailleX, int tailleY, int offsetX, int offsetY, int offsetM, int offsetN) {
    for (int i = 0; i < tailleX; i++) {
      for (int j = 0; j < tailleY; j++) {
        float R = 0f, G = 0f, B = 0f;

        for (int k = 0; k < tailleX; k++) {
          for (int l = 0; l < tailleY; l++) {
            int m = (x + i + k - offsetX + mDimension.width) % mDimension.width;
            int n = (y + j + l - offsetY + mDimension.height) % mDimension.height;

            R += matriceConv[k][l] * mCouleurs[m][n].getRed();
            G += matriceConv[k][l] * mCouleurs[m][n].getGreen();
            B += matriceConv[k][l] * mCouleurs[m][n].getBlue();
          }
        }

        Color lColor = new Color((int) R, (int) G, (int) B);
        mGraphics.setColor(lColor);

        int m = (x + i - offsetM + mDimension.width) % mDimension.width;
        int n = (y + j - offsetN + mDimension.height) % mDimension.height;

        mCouleurs[m][n] = lColor;

        if (!mSuspendu) {
          mGraphics.fillRect(m, n, 1, 1);
        }
      }
    }
  }

  /**
   * Change l'état de suspension
   */
  public void suspendre() {
    mSuspendu = !mSuspendu;
    if (!mSuspendu) {
      repaint();
    }
  }
}
